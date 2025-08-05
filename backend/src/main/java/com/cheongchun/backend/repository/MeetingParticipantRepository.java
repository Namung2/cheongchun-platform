package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.MeetingParticipant;
import com.cheongchun.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    // 기본 조회
    Optional<MeetingParticipant> findByMeetingAndUser(Meeting meeting, User user);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    boolean existsByMeetingAndUser(Meeting meeting, User user);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    // 모임별 참가자 조회
    List<MeetingParticipant> findByMeeting(Meeting meeting);

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    Page<MeetingParticipant> findByMeetingId(Long meetingId, Pageable pageable);

    List<MeetingParticipant> findByMeetingAndStatus(Meeting meeting, MeetingParticipant.Status status);

    List<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, MeetingParticipant.Status status);

    Page<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, MeetingParticipant.Status status, Pageable pageable);
    // 사용자별 참가 모임 조회
    List<MeetingParticipant> findByUser(User user);

    List<MeetingParticipant> findByUserId(Long userId);

    Page<MeetingParticipant> findByUserId(Long userId, Pageable pageable);

    List<MeetingParticipant> findByUserAndStatus(User user, MeetingParticipant.Status status);

    Page<MeetingParticipant> findByUserIdAndStatus(Long userId, MeetingParticipant.Status status, Pageable pageable);

    // 상태별 조회
    Page<MeetingParticipant> findByStatus(MeetingParticipant.Status status, Pageable pageable);

    // 개수 조회
    long countByMeeting(Meeting meeting);

    long countByMeetingId(Long meetingId);

    long countByMeetingAndStatus(Meeting meeting, MeetingParticipant.Status status);

    long countByMeetingIdAndStatus(Long meetingId, MeetingParticipant.Status status);

    long countByUser(User user);

    long countByUserAndStatus(User user, MeetingParticipant.Status status);

    // 승인된 참가자 수 조회 (모임 정원 관리용)
    @Query("SELECT COUNT(p) FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.status = 'APPROVED'")
    long countApprovedParticipants(@Param("meetingId") Long meetingId);

    // 대기 중인 신청 조회
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.status = 'PENDING' ORDER BY p.appliedAt ASC")
    List<MeetingParticipant> findPendingApplications(@Param("meetingId") Long meetingId);

    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.status = 'PENDING' ORDER BY p.appliedAt ASC")
    Page<MeetingParticipant> findPendingApplications(@Param("meetingId") Long meetingId, Pageable pageable);

    // 모임 주최자용 - 참가자 관리
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.createdBy.id = :organizerId AND p.status = 'PENDING' ORDER BY p.appliedAt ASC")
    Page<MeetingParticipant> findPendingApplicationsByOrganizer(@Param("organizerId") Long organizerId, Pageable pageable);

    // 사용자의 참가 이력 (승인된 것만)
    @Query("SELECT p FROM MeetingParticipant p WHERE p.user.id = :userId AND p.status = 'APPROVED' ORDER BY p.meeting.startDate DESC")
    Page<MeetingParticipant> findUserParticipationHistory(@Param("userId") Long userId, Pageable pageable);

    // 사용자의 현재 참가 중인 모임 (진행 예정 + 승인됨)
    @Query("SELECT p FROM MeetingParticipant p WHERE p.user.id = :userId AND p.status = 'APPROVED' AND p.meeting.startDate > CURRENT_TIMESTAMP ORDER BY p.meeting.startDate ASC")
    List<MeetingParticipant> findUserUpcomingMeetings(@Param("userId") Long userId);

    // 모임별 참가자 통계
    @Query("SELECT p.status, COUNT(p) FROM MeetingParticipant p WHERE p.meeting.id = :meetingId GROUP BY p.status")
    List<Object[]> getParticipantStatsByMeeting(@Param("meetingId") Long meetingId);

    // 사용자별 참가 통계
    @Query("SELECT p.status, COUNT(p) FROM MeetingParticipant p WHERE p.user.id = :userId GROUP BY p.status")
    List<Object[]> getParticipantStatsByUser(@Param("userId") Long userId);

    // 모임 주최자가 관리하는 모든 참가 신청
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.createdBy.id = :organizerId ORDER BY p.appliedAt DESC")
    Page<MeetingParticipant> findApplicationsByOrganizer(@Param("organizerId") Long organizerId, Pageable pageable);

    // 특정 기간 내 참가 신청
    @Query("SELECT p FROM MeetingParticipant p WHERE p.appliedAt BETWEEN :startDate AND :endDate")
    List<MeetingParticipant> findApplicationsBetweenDates(@Param("startDate") java.time.LocalDateTime startDate,
                                                          @Param("endDate") java.time.LocalDateTime endDate);

    // 모임 참가자 목록 (승인된 사용자만, 사용자 정보 포함)
    @Query("SELECT p FROM MeetingParticipant p JOIN FETCH p.user WHERE p.meeting.id = :meetingId AND p.status = 'APPROVED'")
    List<MeetingParticipant> findApprovedParticipantsWithUser(@Param("meetingId") Long meetingId);

    // 중복 신청 방지용
    @Query("SELECT COUNT(p) > 0 FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.user.id = :userId AND p.status IN ('PENDING', 'APPROVED')")
    boolean hasActiveApplication(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    // 취소 가능한 참가 신청 조회
    @Query("SELECT p FROM MeetingParticipant p WHERE p.user.id = :userId AND p.status IN ('PENDING', 'APPROVED') AND p.meeting.startDate > CURRENT_TIMESTAMP")
    List<MeetingParticipant> findCancellableApplications(@Param("userId") Long userId);

}