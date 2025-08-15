package com.cheongchun.backend.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AiUserInsightResponse {
    private Long userId;
    private String userName;
    
    // 대화 통계
    private Integer totalConversations;
    private Double averageMessagesPerChat;
    private Double averageDurationMinutes;
    
    // 주요 관심사 (빈도순)
    private List<String> topInterests;
    private List<String> frequentHealthTopics;
    private List<String> recentConcerns;
    
    // 감정/스트레스 패턴
    private String overallMoodTrend; 
    private Double averageStressLevel;
    private List<String> stressFactors;
    
    // 개인화 추천
    private String recommendedConversationStyle;
    private List<String> suggestedTopics;
    private List<String> healthRecommendations;
    
    // 최근 활동
    private LocalDateTime lastChatDate;
    private String lastSummary;
    
    // 시간대별 패턴 (사용자가 주로 채팅하는 시간)
    private Map<String, Integer> chatTimePattern;
}