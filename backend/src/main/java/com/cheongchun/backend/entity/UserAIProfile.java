package com.cheongchun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_ai_profiles")
public class UserAIProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "age_group")
    private String ageGroup;
    
    @Column(name = "health_profile", columnDefinition = "TEXT")
    private String healthProfile; // JSON 형태로 저장
    
    @Column(name = "interests", columnDefinition = "TEXT")
    private String interests; // JSON 형태로 저장
    
    @Column(name = "conversation_style")
    private String conversationStyle = "formal";
    
    @Column(name = "last_summary", columnDefinition = "TEXT")
    private String lastSummary; // 최근 대화 요약
    
    @Column(name = "total_conversations")
    private Integer totalConversations = 0;

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

    // 편의 메서드
    public static UserAIProfile createDefault(User user) {
        UserAIProfile profile = new UserAIProfile();
        profile.setUser(user);
        profile.setConversationStyle("formal");
        profile.setTotalConversations(0);
        return profile;
    }
}