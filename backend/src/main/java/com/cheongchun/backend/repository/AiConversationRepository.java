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
    
    // === 기본 조회 (필수) ===
    List<AiConversation> findTop10ByUserOrderByCreatedAtDesc(User user);
    List<AiConversation> findByUserOrderByCreatedAtDesc(User user);
    List<AiConversation> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime start, LocalDateTime end);
    
    // === 비정규화 컬럼 활용 최적화된 검색 ===
    List<AiConversation> findByUserAndIsHealthRelatedTrueOrderByCreatedAtDesc(User user);
    
    @Query("SELECT ac FROM AiConversation ac WHERE ac.user = :user AND ac.searchKeywords LIKE %:keyword% ORDER BY ac.createdAt DESC")
    List<AiConversation> findByUserAndSearchKeywords(@Param("user") User user, @Param("keyword") String keyword);
    
    List<AiConversation> findByUserAndStressLevelGreaterThanEqualOrderByCreatedAtDesc(User user, Integer minStressLevel);
    List<AiConversation> findByUserAndMoodAnalysisOrderByCreatedAtDesc(User user, String moodAnalysis);
    
    // === 통계 (비정규화 데이터 활용) ===
    @Query("SELECT COUNT(ac), AVG(ac.totalMessages), AVG(ac.durationMinutes) FROM AiConversation ac WHERE ac.user = :user")
    Object[] getUserChatStatistics(@Param("user") User user);
}