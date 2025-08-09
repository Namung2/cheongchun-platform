package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.AiProfileUpdateRequest;
import com.cheongchun.backend.dto.AiConversationRequest;
import com.cheongchun.backend.dto.AiUserInsightResponse;
import com.cheongchun.backend.entity.AiConversation;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.AiConversationRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiUserService {
    
    private final UserRepository userRepository;
    private final AiConversationRepository aiConversationRepository;
    private final ObjectMapper objectMapper;
    
    // 사용자 AI 프로필 조회
    public Map<String, Object> getUserAiProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("name", user.getName());
        profile.put("ageGroup", user.getAiAgeGroup());
        profile.put("conversationStyle", user.getAiConversationStyle());
        profile.put("totalConversations", user.getAiTotalConversations());
        profile.put("lastSummary", user.getAiLastSummary());
        
        // JSON 파싱
        try {
            if (user.getAiHealthProfile() != null) {
                profile.put("healthProfile", objectMapper.readValue(user.getAiHealthProfile(), Map.class));
            }
            if (user.getAiInterests() != null) {
                profile.put("interests", objectMapper.readValue(user.getAiInterests(), List.class));
            }
        } catch (JsonProcessingException e) {
            log.warn("JSON 파싱 실패: {}", e.getMessage());
        }
        
        return profile;
    }
    
    // 사용자 AI 프로필 업데이트
    public void updateUserAiProfile(User user, AiProfileUpdateRequest request) {
        if (request.getAgeGroup() != null) {
            user.setAiAgeGroup(request.getAgeGroup());
        }
        if (request.getConversationStyle() != null) {
            user.setAiConversationStyle(request.getConversationStyle());
        }
        if (request.getHealthProfile() != null) {
            user.setAiHealthProfile(request.getHealthProfile());
        }
        if (request.getInterests() != null) {
            user.setAiInterests(request.getInterests());
        }
        
        userRepository.save(user);
        log.info("사용자 AI 프로필 업데이트: userId={}", user.getId());
    }
    
    // 대화 저장 및 분석
    public Long saveConversation(AiConversationRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // 대화 엔티티 생성 (비정규화로 검색 최적화)
        AiConversation conversation = AiConversation.builder()
            .user(user)
            .sessionTitle(request.getSessionTitle())
            .totalMessages(request.getTotalMessages())
            .durationMinutes(request.getDurationMinutes())
            .messagesJson(request.getMessagesJson())
            .mainTopics(String.join(",", request.getMainTopics() != null ? request.getMainTopics() : Arrays.asList()))
            .healthMentions(String.join(",", request.getHealthMentions() != null ? request.getHealthMentions() : Arrays.asList()))
            .concernsDiscussed(String.join(",", request.getConcernsDiscussed() != null ? request.getConcernsDiscussed() : Arrays.asList()))
            .moodAnalysis(request.getMoodAnalysis())
            .stressLevel(request.getStressLevel())
            .conversationSummary(request.getConversationSummary())
            .build();
        
        // JSON 필드 변환
        try {
            if (request.getKeyInsights() != null) {
                conversation.setKeyInsights(objectMapper.writeValueAsString(request.getKeyInsights()));
            }
            if (request.getAiRecommendations() != null) {
                conversation.setAiRecommendations(objectMapper.writeValueAsString(request.getAiRecommendations()));
            }
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패: {}", e.getMessage());
        }
        
        AiConversation saved = aiConversationRepository.save(conversation);
        
        // 사용자 프로필 업데이트 (캐시 역할)
        user.setAiTotalConversations(user.getAiTotalConversations() + 1);
        user.setAiLastSummary(request.getConversationSummary());
        userRepository.save(user);
        
        log.info("대화 저장 완료: conversationId={}, userId={}", saved.getId(), user.getId());
        return saved.getId();
    }
    
    // 사용자 인사이트 생성 (빠른 검색을 위한 집계)
    public AiUserInsightResponse getUserInsights(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        // 최근 30일간 대화들
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AiConversation> recentConversations = aiConversationRepository
            .findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, thirtyDaysAgo, LocalDateTime.now());
        
        // 통계 계산
        Object[] stats = aiConversationRepository.getUserChatStatistics(user);
        Long totalConversations = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        Double avgMessages = stats[1] != null ? ((Number) stats[1]).doubleValue() : 0.0;
        Double avgDuration = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;
        
        // 주제 빈도 분석 (비정규화된 데이터 활용)
        Map<String, Long> topicFrequency = recentConversations.stream()
            .flatMap(conv -> Arrays.stream(conv.getMainTopics().split(",")))
            .filter(topic -> !topic.trim().isEmpty())
            .collect(Collectors.groupingBy(topic -> topic.trim(), Collectors.counting()));
        
        List<String> topInterests = topicFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // 건강 주제 빈도
        List<String> healthTopics = recentConversations.stream()
            .filter(conv -> conv.getHealthMentions() != null && !conv.getHealthMentions().isEmpty())
            .flatMap(conv -> Arrays.stream(conv.getHealthMentions().split(",")))
            .filter(topic -> !topic.trim().isEmpty())
            .collect(Collectors.groupingBy(topic -> topic.trim(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // 스트레스 레벨 평균
        Double avgStress = recentConversations.stream()
            .filter(conv -> conv.getStressLevel() != null)
            .mapToInt(AiConversation::getStressLevel)
            .average()
            .orElse(0.0);
        
        // 최근 대화 정보
        AiConversation lastConversation = recentConversations.isEmpty() ? null : recentConversations.get(0);
        
        return AiUserInsightResponse.builder()
            .userId(userId)
            .userName(user.getName())
            .totalConversations(totalConversations.intValue())
            .averageMessagesPerChat(avgMessages)
            .averageDurationMinutes(avgDuration)
            .topInterests(topInterests)
            .frequentHealthTopics(healthTopics)
            .averageStressLevel(avgStress)
            .lastChatDate(lastConversation != null ? lastConversation.getCreatedAt() : null)
            .lastSummary(user.getAiLastSummary())
            .recommendedConversationStyle(user.getAiConversationStyle())
            .build();
    }
    
    // 채팅 히스토리 조회 (AI Core에서 사용)
    public List<Map<String, Object>> getChatHistory(Long userId, int limit) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        List<AiConversation> conversations = aiConversationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        
        return conversations.stream()
            .limit(limit)
            .map(conv -> {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("id", conv.getId());
                chatData.put("title", conv.getSessionTitle());
                chatData.put("summary", conv.getConversationSummary());
                chatData.put("topics", Arrays.asList(conv.getMainTopics().split(",")));
                chatData.put("createdAt", conv.getCreatedAt());
                return chatData;
            })
            .collect(Collectors.toList());
    }
    
    // 건강 관련 요약
    public Map<String, Object> getHealthSummary(User user) {
        List<AiConversation> healthConversations = aiConversationRepository.findHealthRelatedConversations(user);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHealthChats", healthConversations.size());
        
        if (!healthConversations.isEmpty()) {
            // 최근 건강 대화 요약
            List<String> recentSummaries = healthConversations.stream()
                .limit(5)
                .map(AiConversation::getConversationSummary)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            summary.put("recentHealthSummaries", recentSummaries);
            
            // 주요 건강 관심사
            Set<String> healthConcerns = healthConversations.stream()
                .flatMap(conv -> Arrays.stream(conv.getHealthMentions().split(",")))
                .filter(concern -> !concern.trim().isEmpty())
                .collect(Collectors.toSet());
            summary.put("mainHealthConcerns", new ArrayList<>(healthConcerns));
        }
        
        return summary;
    }
}