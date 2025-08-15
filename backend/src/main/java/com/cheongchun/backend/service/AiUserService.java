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
    
    // 대화 저장 및 AI 요약 생성
    public Long saveConversation(AiConversationRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        // GPT API 요약 생성을 위한 AI Core 서비스 호출
        String summary = request.getConversationSummary() != null ? 
            request.getConversationSummary() : "대화 요약을 생성 중입니다.";
            
        // 비정규화 데이터 계산
        String mainTopicsStr = String.join(",", request.getMainTopics() != null ? request.getMainTopics() : Collections.emptyList());
        String healthMentionsStr = String.join(",", request.getHealthMentions() != null ? request.getHealthMentions() : Collections.emptyList());
        String concernsStr = String.join(",", request.getConcernsDiscussed() != null ? request.getConcernsDiscussed() : Collections.emptyList());
        
        // 검색 키워드 생성 (모든 텍스트 합침)
        String searchKeywords = generateSearchKeywords(
            request.getSessionTitle(), 
            mainTopicsStr, 
            healthMentionsStr, 
            summary
        );
        
        // 통계 정보 계산
        int topicCount = request.getMainTopics() != null ? request.getMainTopics().size() : 0;
        int healthKeywordCount = request.getHealthMentions() != null ? request.getHealthMentions().size() : 0;
        boolean isHealthRelated = healthKeywordCount > 0 || (summary != null && containsHealthKeywords(summary));
        
        // 대화 엔티티 생성 (비정규화 데이터 포함)
        AiConversation conversation = AiConversation.builder()
            .user(user)
            .userName(user.getName()) // 비정규화
            .userAgeGroup(user.getAiAgeGroup()) // 비정규화
            .sessionTitle(request.getSessionTitle())
            .totalMessages(request.getTotalMessages())
            .durationMinutes(request.getDurationMinutes())
            .messagesJson(null) // 원본 메시지는 저장하지 않음
            .mainTopics(mainTopicsStr)
            .healthMentions(healthMentionsStr)
            .concernsDiscussed(concernsStr)
            .moodAnalysis(request.getMoodAnalysis() != null ? request.getMoodAnalysis() : "neutral")
            .stressLevel(request.getStressLevel() != null ? request.getStressLevel() : 5)
            .conversationSummary(summary)
            .searchKeywords(searchKeywords) // 검색 최적화
            .isHealthRelated(isHealthRelated) // 검색 최적화
            .topicCount(topicCount) // 통계 정보
            .healthKeywordCount(healthKeywordCount) // 통계 정보
            .summaryLength(summary != null ? summary.length() : 0) // 통계 정보
            .build();
        
        // AI 생성 데이터를 JSON으로 변환
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
        Integer currentCount = user.getAiTotalConversations();
        user.setAiTotalConversations((currentCount != null ? currentCount : 0) + 1);
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
        
        // 비정규화 데이터로 빠른 통계 계산
        List<String> topInterests = recentConversations.stream()
            .filter(conv -> conv.getTopicCount() > 0)
            .flatMap(conv -> Arrays.stream(conv.getMainTopics().split(",")))
            .filter(topic -> !topic.trim().isEmpty())
            .collect(Collectors.groupingBy(topic -> topic.trim(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // 건강 관련 대화만 필터링하여 건강 키워드 추출
        List<String> healthTopics = recentConversations.stream()
            .filter(conv -> conv.getIsHealthRelated() && conv.getHealthKeywordCount() > 0)
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
    
    // 채팅 히스토리 조회 (AI Core에서 사용) - 비정규화 데이터 활용
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
                chatData.put("userName", conv.getUserName()); // 비정규화 데이터
                chatData.put("isHealthRelated", conv.getIsHealthRelated()); // 비정규화 데이터
                chatData.put("topicCount", conv.getTopicCount()); // 비정규화 데이터
                chatData.put("createdAt", conv.getCreatedAt());
                return chatData;
            })
            .collect(Collectors.toList());
    }
    
    // 건강 관련 요약 (비정규화 컬럼 활용)
    public Map<String, Object> getHealthSummary(User user) {
        List<AiConversation> healthConversations = aiConversationRepository.findByUserAndIsHealthRelatedTrueOrderByCreatedAtDesc(user);
        
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
            
            // 주요 건강 관심사 (비정규화 데이터 활용)
            Set<String> healthConcerns = healthConversations.stream()
                .filter(conv -> conv.getHealthKeywordCount() > 0) // 비정규화 필터
                .flatMap(conv -> Arrays.stream(conv.getHealthMentions().split(",")))
                .filter(concern -> !concern.trim().isEmpty())
                .collect(Collectors.toSet());
            summary.put("mainHealthConcerns", new ArrayList<>(healthConcerns));
        }
        
        return summary;
    }
    
    // 대화 삭제
    public void deleteConversation(User user, Long conversationId) {
        AiConversation conversation = aiConversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("대화를 찾을 수 없습니다: " + conversationId));
        
        // 본인의 대화인지 확인
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("본인의 대화만 삭제할 수 있습니다.");
        }
        
        aiConversationRepository.delete(conversation);
        
        // 사용자 통계 업데이트
        if (user.getAiTotalConversations() > 0) {
            user.setAiTotalConversations(user.getAiTotalConversations() - 1);
            userRepository.save(user);
        }
        
        log.info("대화 삭제 완료: conversationId={}, userId={}", conversationId, user.getId());
    }
    
    // 사용자의 모든 대화 기록 조회 (페이징 처리)
    public List<Map<String, Object>> getUserConversations(User user, int page, int size) {
        List<AiConversation> conversations = aiConversationRepository.findByUserOrderByCreatedAtDesc(user);
        
        List<AiConversation> pagedConversations = conversations.stream()
            .skip((long) page * size)
            .limit(size)
            .collect(Collectors.toList());
            
        return convertToConversationDataList(pagedConversations);
    }
    
    // ===== 비정규화 데이터 처리 헬퍼 메서드들 =====
    
    // 검색 키워드 생성 (모든 텍스트를 공백으로 구분하여 합침)
    private String generateSearchKeywords(String title, String topics, String healthMentions, String summary) {
        StringBuilder keywords = new StringBuilder();
        
        if (title != null && !title.trim().isEmpty()) {
            keywords.append(title.trim()).append(" ");
        }
        if (topics != null && !topics.trim().isEmpty()) {
            keywords.append(topics.replace(",", " ")).append(" ");
        }
        if (healthMentions != null && !healthMentions.trim().isEmpty()) {
            keywords.append(healthMentions.replace(",", " ")).append(" ");
        }
        if (summary != null && !summary.trim().isEmpty()) {
            // 요약문에서 핵심 키워드만 추출 (첫 100자)
            String summaryKeywords = summary.length() > 100 ? summary.substring(0, 100) : summary;
            keywords.append(summaryKeywords.trim());
        }
        
        return keywords.toString().trim().toLowerCase(); // 소문자로 통일
    }
    
    // 건강 관련 키워드 포함 여부 검사
    private boolean containsHealthKeywords(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        
        String[] healthKeywords = {
            "건강", "병원", "의사", "약", "치료", "진료", "검사",
            "혈압", "당뇨", "관절", "심장", "뇌", "간", "신장",
            "운동", "식단", "영양", "수면", "스트레스",
            "아프", "통증", "피로", "어지러"
        };
        
        String lowerText = text.toLowerCase();
        for (String keyword : healthKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    // ===== 비정규화 컬럼 활용 빠른 검색 메서드들 =====
    
    // 키워드로 대화 검색 (단일 테이블, 단일 LIKE 검색)
    public List<Map<String, Object>> searchConversations(User user, String keyword) {
        List<AiConversation> conversations = aiConversationRepository.findByUserAndSearchKeywords(user, keyword.toLowerCase());
        
        return convertToConversationDataList(conversations);
    }
    
    // 건강 관련 대화만 조회 (boolean 컬럼 활용)
    public List<Map<String, Object>> getHealthConversations(User user) {
        List<AiConversation> conversations = aiConversationRepository.findByUserAndIsHealthRelatedTrueOrderByCreatedAtDesc(user);
        
        return convertToConversationDataList(conversations);
    }
    
    // 스트레스 레벨별 대화 조회 (비정규화 최적화)
    public List<Map<String, Object>> getHighStressConversations(User user, int minStressLevel) {
        List<AiConversation> conversations = aiConversationRepository.findByUserAndStressLevelGreaterThanEqualOrderByCreatedAtDesc(user, minStressLevel);
        return convertToConversationDataList(conversations);
    }
    
    // 감정 상태별 대화 조회 (비정규화 최적화)
    public List<Map<String, Object>> getConversationsByMood(User user, String mood) {
        List<AiConversation> conversations = aiConversationRepository.findByUserAndMoodAnalysisOrderByCreatedAtDesc(user, mood);
        return convertToConversationDataList(conversations);
    }
    
    // 대화 데이터 변환 공통 메서드
    private List<Map<String, Object>> convertToConversationDataList(List<AiConversation> conversations) {
        return conversations.stream()
            .map(conv -> {
                Map<String, Object> conversationData = new HashMap<>();
                conversationData.put("id", conv.getId());
                conversationData.put("title", conv.getSessionTitle());
                conversationData.put("summary", conv.getConversationSummary());
                conversationData.put("userName", conv.getUserName()); // 비정규화 데이터
                conversationData.put("userAgeGroup", conv.getUserAgeGroup()); // 비정규화 데이터
                conversationData.put("topics", Arrays.asList(conv.getMainTopics().split(",")));
                conversationData.put("healthMentions", Arrays.asList(conv.getHealthMentions().split(",")));
                conversationData.put("moodAnalysis", conv.getMoodAnalysis());
                conversationData.put("stressLevel", conv.getStressLevel());
                conversationData.put("totalMessages", conv.getTotalMessages());
                conversationData.put("durationMinutes", conv.getDurationMinutes());
                conversationData.put("isHealthRelated", conv.getIsHealthRelated()); // 비정규화 데이터
                conversationData.put("topicCount", conv.getTopicCount()); // 비정규화 데이터
                conversationData.put("healthKeywordCount", conv.getHealthKeywordCount()); // 비정규화 데이터
                conversationData.put("summaryLength", conv.getSummaryLength()); // 비정규화 데이터
                conversationData.put("createdAt", conv.getCreatedAt());
                
                // JSON 필드 파싱
                try {
                    if (conv.getKeyInsights() != null) {
                        conversationData.put("keyInsights", objectMapper.readValue(conv.getKeyInsights(), List.class));
                    }
                    if (conv.getAiRecommendations() != null) {
                        conversationData.put("aiRecommendations", objectMapper.readValue(conv.getAiRecommendations(), List.class));
                    }
                } catch (JsonProcessingException e) {
                    log.warn("JSON 파싱 실패: {}", e.getMessage());
                }
                
                return conversationData;
            })
            .collect(Collectors.toList());
    }
}