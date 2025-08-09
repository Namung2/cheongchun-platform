package com.cheongchun.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_conversations", indexes = {
    @Index(name = "idx_user_date", columnList = "user_id, created_at"),
    @Index(name = "idx_user_topics", columnList = "user_id, main_topics"),
    @Index(name = "idx_health_mentions", columnList = "health_mentions")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // 대화 기본 정보 (비정규화로 빠른 조회)
    @Column(name = "session_title")
    private String sessionTitle;
    
    @Column(name = "total_messages")
    private Integer totalMessages = 0;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 0;
    
    // 대화 내용 (JSON 형태로 저장 - 검색보다는 저장 우선)
    @Column(name = "messages_json", columnDefinition = "TEXT")
    private String messagesJson;
    
    // 분석 결과 (비정규화로 빠른 검색)
    @Column(name = "main_topics")
    private String mainTopics; // "건강,가족,취미" 쉼표 구분
    
    @Column(name = "health_mentions")  
    private String healthMentions; // "혈압,당뇨,운동" 쉼표 구분
    
    @Column(name = "concerns_discussed")
    private String concernsDiscussed; // "외로움,건강염려" 쉼표 구분
    
    @Column(name = "mood_analysis")
    private String moodAnalysis; // "positive", "neutral", "concerned"
    
    @Column(name = "stress_level")
    private Integer stressLevel; // 1-10
    
    // AI 생성 요약 (검색용)
    @Column(name = "conversation_summary", columnDefinition = "TEXT")
    private String conversationSummary;
    
    @Column(name = "key_insights", columnDefinition = "TEXT") 
    private String keyInsights; // JSON 배열
    
    @Column(name = "ai_recommendations", columnDefinition = "TEXT")
    private String aiRecommendations; // JSON 배열
    
    // 시간 정보
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}