package com.cheongchun.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiConversationRequest {
    private Long userId;
    private String sessionTitle;
    private Integer totalMessages;
    private Integer durationMinutes;
    private String messagesJson; // 전체 대화 내용
    
    // AI 분석 결과
    private List<String> mainTopics;
    private List<String> healthMentions;  
    private List<String> concernsDiscussed;
    private String moodAnalysis;
    private Integer stressLevel;
    private String conversationSummary;
    private List<String> keyInsights;
    private List<String> aiRecommendations;
}