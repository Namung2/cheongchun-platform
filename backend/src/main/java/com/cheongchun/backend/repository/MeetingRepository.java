package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // 기본 조회
    Page<Meeting> findByStatus(Meeting.Status status, Pageable pageable);

    Page<Meeting> findByCategory(Meeting.Category category, Pageable pageable);

    Page<Meeting> findByCategoryAndStatus(Meeting.Category category, Meeting.Status status, Pageable pageable);

    // 지역 기반 검색
    Page<Meeting> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    @Query("SELECT m FROM Meeting m WHERE m.location LIKE %:location% AND m.status = :status")
    Page<Meeting> findByLocationAndStatus(@Param("location") String location,
                                          @Param("status") Meeting.Status status,
                                          Pageable pageable);

    // 날짜 기반 검색
    @Query("SELECT m FROM Meeting m WHERE m.startDate >= :startDate AND m.status = :status")
    Page<Meeting> findUpcomingMeetings(@Param("startDate") LocalDateTime startDate,
                                       @Param("status") Meeting.Status status,
                                       Pageable pageable);

    @Query("SELECT m FROM Meeting m WHERE m.startDate BETWEEN :startDate AND :endDate AND m.status = :status")
    Page<Meeting> findMeetingsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           @Param("status") Meeting.Status status,
                                           Pageable pageable);

    // 복합 검색
    @Query("SELECT m FROM Meeting m WHERE " +
            "(:category IS NULL OR m.category = :category) AND " +
            "(:location IS NULL OR m.location LIKE %:location%) AND " +
            "(:status IS NULL OR m.status = :status) AND " +
            "(:startDate IS NULL OR m.startDate >= :startDate)")
    Page<Meeting> findMeetingsWithFilters(@Param("category") Meeting.Category category,
                                          @Param("location") String location,
                                          @Param("status") Meeting.Status status,
                                          @Param("startDate") LocalDateTime startDate,
                                          Pageable pageable);

    // 텍스트 검색 (제목, 설명)
    @Query("SELECT m FROM Meeting m WHERE " +
            "(m.title LIKE %:keyword% OR m.description LIKE %:keyword%) AND " +
            "m.status = :status")
    Page<Meeting> searchByKeyword(@Param("keyword") String keyword,
                                  @Param("status") Meeting.Status status,
                                  Pageable pageable);

    // 인기 모임 (일일 조회수 기준)
    @Query("SELECT m FROM Meeting m WHERE m.status = :status ORDER BY m.dailyViewCount DESC")
    Page<Meeting> findTodayBestMeetings(@Param("status") Meeting.Status status, Pageable pageable);

    // 전체 조회수 기준 인기 모임
    @Query("SELECT m FROM Meeting m WHERE m.status = :status ORDER BY m.viewCount DESC")
    Page<Meeting> findPopularMeetings(@Param("status") Meeting.Status status, Pageable pageable);

    // 최신 모임
    @Query("SELECT m FROM Meeting m WHERE m.status = :status ORDER BY m.createdAt DESC")
    Page<Meeting> findLatestMeetings(@Param("status") Meeting.Status status, Pageable pageable);

    // 사용자가 생성한 모임
    Page<Meeting> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);

    @Query("SELECT m FROM Meeting m WHERE m.createdBy = :user AND m.status = :status ORDER BY m.createdAt DESC")
    Page<Meeting> findByCreatedByAndStatus(@Param("user") User user,
                                           @Param("status") Meeting.Status status,
                                           Pageable pageable);

    // 참가비 기반 검색
    Page<Meeting> findByFeeAndStatus(Integer fee, Meeting.Status status, Pageable pageable);

    @Query("SELECT m FROM Meeting m WHERE m.fee <= :maxFee AND m.status = :status")
    Page<Meeting> findByMaxFee(@Param("maxFee") Integer maxFee,
                               @Param("status") Meeting.Status status,
                               Pageable pageable);

    // 난이도별 검색
    Page<Meeting> findByDifficultyLevelAndStatus(Meeting.DifficultyLevel difficultyLevel,
                                                 Meeting.Status status,
                                                 Pageable pageable);

    // 통계 관련
    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.status = :status")
    long countByStatus(@Param("status") Meeting.Status status);

    @Query("SELECT m.category, COUNT(m) FROM Meeting m WHERE m.status = :status GROUP BY m.category")
    List<Object[]> countByCategory(@Param("status") Meeting.Status status);

    // 조회수 증가
    @Modifying
    @Query("UPDATE Meeting m SET m.viewCount = m.viewCount + 1, m.dailyViewCount = m.dailyViewCount + 1 WHERE m.id = :meetingId")
    void incrementViewCount(@Param("meetingId") Long meetingId);

    // 찜 개수 업데이트
    @Modifying
    @Query("UPDATE Meeting m SET m.wishlistCount = (SELECT COUNT(w) FROM UserWishlist w WHERE w.meeting.id = m.id) WHERE m.id = :meetingId")
    void updateWishlistCount(@Param("meetingId") Long meetingId);

    // 참가자 수 업데이트
    @Modifying
    @Query("UPDATE Meeting m SET m.currentParticipants = (SELECT COUNT(p) FROM MeetingParticipant p WHERE p.meeting.id = m.id AND p.status = 'APPROVED') WHERE m.id = :meetingId")
    void updateParticipantCount(@Param("meetingId") Long meetingId);

    // 일일 조회수 초기화 (매일 자정에 실행)
    @Modifying
    @Query("UPDATE Meeting m SET m.dailyViewCount = 0")
    void resetDailyViewCount();

    // 만료된 모임 상태 업데이트
    @Modifying
    @Query("UPDATE Meeting m SET m.status = 'CLOSED' WHERE m.endDate < :now AND m.status IN ('RECRUITING', 'FULL')")
    void closeExpiredMeetings(@Param("now") LocalDateTime now);

    // 고급 검색 (가격 범위, 날짜 범위, 지역, 카테고리 모두 포함)
    @Query("SELECT m FROM Meeting m WHERE " +
            "(:category IS NULL OR m.category = :category) AND " +
            "(:subcategory IS NULL OR m.subcategory = :subcategory) AND " +
            "(:location IS NULL OR m.location LIKE %:location%) AND " +
            "(:minFee IS NULL OR m.fee >= :minFee) AND " +
            "(:maxFee IS NULL OR m.fee <= :maxFee) AND " +
            "(:difficulty IS NULL OR m.difficultyLevel = :difficulty) AND " +
            "(:startDate IS NULL OR m.startDate >= :startDate) AND " +
            "(:endDate IS NULL OR m.startDate <= :endDate) AND " +
            "m.status = :status")
    Page<Meeting> findMeetingsWithAdvancedFilters(
            @Param("category") Meeting.Category category,
            @Param("subcategory") String subcategory,
            @Param("location") String location,
            @Param("minFee") Integer minFee,
            @Param("maxFee") Integer maxFee,
            @Param("difficulty") Meeting.DifficultyLevel difficulty,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Meeting.Status status,
            Pageable pageable
    );

    // 사용자 관련 조회 (추후 AI 추천에 사용)
    @Query("SELECT m FROM Meeting m WHERE m.id IN :meetingIds")
    List<Meeting> findByIdIn(@Param("meetingIds") List<Long> meetingIds);

    // 중복 확인
    boolean existsByTitleAndCreatedByAndStartDate(String title, User createdBy, LocalDateTime startDate);

    // 특정 사용자가 참여한 모임들
    @Query("SELECT m FROM Meeting m JOIN MeetingParticipant p ON m.id = p.meeting.id WHERE p.user.id = :userId AND p.status = 'APPROVED'")
    Page<Meeting> findMeetingsByParticipantUserId(@Param("userId") Long userId, Pageable pageable);

    // 특정 사용자가 찜한 모임들
    @Query("SELECT m FROM Meeting m JOIN UserWishlist w ON m.id = w.meeting.id WHERE w.user.id = :userId")
    Page<Meeting> findMeetingsByWishlistUserId(@Param("userId") Long userId, Pageable pageable);
}