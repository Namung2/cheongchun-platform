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
    
    // === 사용자 정보 비정규화 (JOIN 제거) ===
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "user_age_group", length = 20)
    private String userAgeGroup;
    
    // 대화 기본 정보 (비정규화로 빠른 조회)
    @Column(name = "session_title")
    private String sessionTitle;
    
    @Builder.Default
    @Column(name = "total_messages")
    private Integer totalMessages = 0;
    
    @Builder.Default
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
    private String aiRecommendations;
    
    // === 검색 최적화 컬럼 ===
    @Column(name = "search_keywords", columnDefinition = "TEXT")
    private String searchKeywords; // 모든 검색 키워드 합침 (제목+주제+건강+요약)
    
    @Builder.Default
    @Column(name = "is_health_related")
    private Boolean isHealthRelated = false; // 건강 관련 대화 여부
    
    // === 통계 정보 사전 계산 ===
    @Builder.Default
    @Column(name = "topic_count")
    private Integer topicCount = 0; // 주제 개수
    
    @Builder.Default
    @Column(name = "health_keyword_count")  
    private Integer healthKeywordCount = 0; // 건강 키워드 개수
    
    @Builder.Default
    @Column(name = "summary_length")
    private Integer summaryLength = 0; // 요약문 길이 (정렬용)
    
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