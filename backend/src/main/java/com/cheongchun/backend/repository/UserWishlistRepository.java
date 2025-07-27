package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.entity.UserWishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWishlistRepository extends JpaRepository<UserWishlist, Long> {

    // 기본 조회
    Optional<UserWishlist> findByUserAndMeeting(User user, Meeting meeting);

    Optional<UserWishlist> findByUserIdAndMeetingId(Long userId, Long meetingId);

    boolean existsByUserAndMeeting(User user, Meeting meeting);

    boolean existsByUserIdAndMeetingId(Long userId, Long meetingId);

    // 사용자별 찜 목록
    List<UserWishlist> findByUser(User user);

    List<UserWishlist> findByUserId(Long userId);

    Page<UserWishlist> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT w FROM UserWishlist w ORDER BY w.createdAt DESC")
    Page<UserWishlist> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 모임별 찜한 사용자 목록
    List<UserWishlist> findByMeeting(Meeting meeting);

    List<UserWishlist> findByMeetingId(Long meetingId);

    Page<UserWishlist> findByMeetingId(Long meetingId, Pageable pageable);

    // 개수 조회
    long countByUser(User user);

    long countByUserId(Long userId);

    long countByMeeting(Meeting meeting);

    long countByMeetingId(Long meetingId);

    // 사용자의 찜 목록 (모임 정보 포함)
    @Query("SELECT w FROM UserWishlist w JOIN FETCH w.meeting WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<UserWishlist> findByUserIdWithMeeting(@Param("userId") Long userId);

    @Query("SELECT w FROM UserWishlist w JOIN FETCH w.meeting WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    Page<UserWishlist> findByUserIdWithMeeting(@Param("userId") Long userId, Pageable pageable);

    // 카테고리별 찜 목록
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND w.meeting.category = :category ORDER BY w.createdAt DESC")
    Page<UserWishlist> findByUserIdAndMeetingCategory(@Param("userId") Long userId,
                                                      @Param("category") Meeting.Category category,
                                                      Pageable pageable);

    // 상태별 찜 목록 (모집중인 모임만)
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND w.meeting.status = :status ORDER BY w.createdAt DESC")
    Page<UserWishlist> findByUserIdAndMeetingStatus(@Param("userId") Long userId,
                                                    @Param("status") Meeting.Status status,
                                                    Pageable pageable);

    // 지역별 찜 목록
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND w.meeting.location LIKE %:location% ORDER BY w.createdAt DESC")
    Page<UserWishlist> findByUserIdAndMeetingLocation(@Param("userId") Long userId,
                                                      @Param("location") String location,
                                                      Pageable pageable);

    // 찜한 모임 중 곧 시작되는 모임
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND w.meeting.startDate > CURRENT_TIMESTAMP AND w.meeting.status = 'RECRUITING' ORDER BY w.meeting.startDate ASC")
    List<UserWishlist> findUpcomingWishlistedMeetings(@Param("userId") Long userId);

    // 찜 통계 - 카테고리별 개수
    @Query("SELECT w.meeting.category, COUNT(w) FROM UserWishlist w WHERE w.user.id = :userId GROUP BY w.meeting.category")
    List<Object[]> getWishlistStatsByCategory(@Param("userId") Long userId);

    // 인기 모임 (찜이 많은 순)
    @Query("SELECT w.meeting, COUNT(w) as wishlistCount FROM UserWishlist w GROUP BY w.meeting ORDER BY wishlistCount DESC")
    Page<Object[]> findPopularMeetingsByWishlist(Pageable pageable);

    // 사용자의 찜 목록에서 ID만 조회 (AI 추천용)
    @Query("SELECT w.meeting.id FROM UserWishlist w WHERE w.user.id = :userId")
    List<Long> findMeetingIdsByUserId(@Param("userId") Long userId);

    // 비슷한 취향의 사용자들이 찜한 모임 (협업 필터링용)
    @Query("SELECT w.meeting.id, COUNT(w) FROM UserWishlist w WHERE w.user.id IN :userIds GROUP BY w.meeting.id ORDER BY COUNT(w) DESC")
    List<Object[]> findPopularMeetingsAmongUsers(@Param("userIds") List<Long> userIds, Pageable pageable);

    // 사용자가 찜했지만 아직 신청하지 않은 모임
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND NOT EXISTS (SELECT p FROM MeetingParticipant p WHERE p.user.id = :userId AND p.meeting.id = w.meeting.id)")
    List<UserWishlist> findWishlistedButNotApplied(@Param("userId") Long userId);

    // 찜한 모임 중 참가비가 무료인 모임
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND w.meeting.fee = 0 ORDER BY w.createdAt DESC")
    List<UserWishlist> findFreeWishlistedMeetings(@Param("userId") Long userId);

    // 특정 기간에 찜한 모임들
    @Query("SELECT w FROM UserWishlist w WHERE w.user.id = :userId AND w.createdAt BETWEEN :startDate AND :endDate ORDER BY w.createdAt DESC")
    List<UserWishlist> findWishlistBetweenDates(@Param("userId") Long userId,
                                                @Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);

    // 찜 목록 정리 (만료된 모임 제거)
    @Query("DELETE FROM UserWishlist w WHERE w.meeting.endDate < CURRENT_TIMESTAMP")
    void deleteExpiredWishlists();
}