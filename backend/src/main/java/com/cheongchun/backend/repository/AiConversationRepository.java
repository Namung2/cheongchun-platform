package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.AiConversation;
import com.cheongchun.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    
    // 사용자별 최근 대화 조회 (빠른 검색을 위한 인덱스 활용)
    List<AiConversation> findTop10ByUserOrderByCreatedAtDesc(User user);
    
    // 특정 주제로 대화 검색 (LIKE 검색 최소화)
    @Query("SELECT ac FROM AiConversation ac WHERE ac.user = :user AND ac.mainTopics LIKE %:topic% ORDER BY ac.createdAt DESC")
    List<AiConversation> findByUserAndMainTopicsContaining(@Param("user") User user, @Param("topic") String topic);
    
    // 건강 관련 대화만 조회
    @Query("SELECT ac FROM AiConversation ac WHERE ac.user = :user AND ac.healthMentions IS NOT NULL AND ac.healthMentions != '' ORDER BY ac.createdAt DESC")
    List<AiConversation> findHealthRelatedConversations(@Param("user") User user);
    
    // 기간별 대화 조회
    List<AiConversation> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime start, LocalDateTime end);
    
    // 사용자 대화 통계 (집계)
    @Query("SELECT COUNT(ac), AVG(ac.totalMessages), AVG(ac.durationMinutes) FROM AiConversation ac WHERE ac.user = :user")
    Object[] getUserChatStatistics(@Param("user") User user);
    
    // 최근 N일간 대화 요약
    @Query("SELECT ac.conversationSummary FROM AiConversation ac WHERE ac.user = :user AND ac.createdAt >= :since ORDER BY ac.createdAt DESC")
    List<String> getRecentSummaries(@Param("user") User user, @Param("since") LocalDateTime since);
}