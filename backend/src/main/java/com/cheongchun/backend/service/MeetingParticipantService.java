package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.MeetingParticipantDto;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.MeetingParticipant;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.MeetingParticipantRepository;
import com.cheongchun.backend.repository.MeetingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class MeetingParticipantService {

    private final MeetingParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;

    private final Map<Long, Integer> autoApprovalLimits = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> autoApprovalCounts = new ConcurrentHashMap<>();

    public MeetingParticipantService(MeetingParticipantRepository participantRepository,
                                     MeetingRepository meetingRepository) {
        this.participantRepository = participantRepository;
        this.meetingRepository = meetingRepository;
    }

    // 모임 생성 시 자동승인 설정 저장
    public void initAutoApproval(Long meetingId, Integer autoApprovalLimit) {
        if (autoApprovalLimit != null && autoApprovalLimit > 0) {
            autoApprovalLimits.put(meetingId, autoApprovalLimit);
            autoApprovalCounts.put(meetingId, new AtomicInteger(0));
        }
    }

    /**
     * 모임 참여 신청
     */
    // joinMeeting 메서드 - 자동승인 로직 직접 처리
    public MeetingParticipantDto.ParticipantResponse joinMeeting(Long meetingId,
                                                                 MeetingParticipantDto.JoinRequest request,
                                                                 User currentUser) {
        // 기존 검증 로직...
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("모임을 찾을 수 없습니다"));

        validateJoinRequest(meeting, currentUser);

        if (participantRepository.existsByMeetingAndUser(meeting, currentUser)) {
            throw new RuntimeException("이미 신청한 모임입니다");
        }

        // 참가 신청 생성
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeeting(meeting);
        participant.setUser(currentUser);
        participant.setApplicationMessage(request.getApplicationMessage());

        // 자동승인 확인 및 직접 처리
        if (isAutoApprovalEnabled(meeting)) {
            // 자동승인 처리
            participant.setStatus(MeetingParticipant.Status.APPROVED);
            participant.setProcessedAt(LocalDateTime.now());

            // 자동승인 카운트 증가
            AtomicInteger count = autoApprovalCounts.get(meeting.getId());
            if (count != null) {
                count.incrementAndGet();
            }
        } else {
            // 수동승인 대기
            participant.setStatus(MeetingParticipant.Status.PENDING);
        }

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        // 승인된 경우에만 참가자 수 업데이트
        if (savedParticipant.getStatus() == MeetingParticipant.Status.APPROVED) {
            updateMeetingParticipantCount(meetingId);
        }

        return convertToParticipantResponse(savedParticipant);
    }

    // 메모리 정리 (모임 삭제 시 호출)
    public void cleanupAutoApprovalData(Long meetingId) {
        autoApprovalLimits.remove(meetingId);
        autoApprovalCounts.remove(meetingId);
    }

    /**
     * 참여 신청 취소
     */
    public void cancelApplication(Long meetingId, User currentUser) {
        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("참여 신청 내역을 찾을 수 없습니다"));

        // 취소 가능 여부 확인
        if (!participant.canCancel()) {
            throw new RuntimeException("취소할 수 없는 상태입니다");
        }

        // 모임 시작 시간 확인 (1시간 전까지만 취소 가능)
        if (participant.getMeeting().getStartDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new RuntimeException("모임 시작 1시간 전부터는 참여를 취소할 수 없습니다");
        }

        participant.cancel();
        participantRepository.save(participant);

        // 참가자 수 업데이트
        if (participant.getStatus() == MeetingParticipant.Status.APPROVED) {
            updateMeetingParticipantCount(meetingId);
        }
    }
    /**
     * 참여 신청 승인 (주최자용)
     */

    public MeetingParticipantDto.ParticipantResponse approveApplication(Long participantId, User currentUser) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여 신청을 찾을 수 없습니다"));

        // 권한 확인 (주최자 또는 관리자)
        validateOrganizerPermission(participant.getMeeting(), currentUser);

        // 상태 확인
        if (participant.getStatus() != MeetingParticipant.Status.PENDING) {
            throw new RuntimeException("승인 대기 중인 신청이 아닙니다");
        }

        // 정원 확인
        if (!participant.getMeeting().canJoin()) {
            throw new RuntimeException("모임 정원이 가득 찼습니다");
        }

        // 승인 처리
        participant.approve(currentUser.getId());
        MeetingParticipant savedParticipant = participantRepository.save(participant);

        // 모임 참가자 수 업데이트
        updateMeetingParticipantCount(participant.getMeeting().getId());

        return convertToParticipantResponse(savedParticipant);
    }

    /**
     * 참여 신청 거절 (주최자용)
     */
    public MeetingParticipantDto.ParticipantResponse rejectApplication(Long participantId,
                                                                       MeetingParticipantDto.RejectRequest request,
                                                                       User currentUser) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여 신청을 찾을 수 없습니다"));

        // 권한 확인
        validateOrganizerPermission(participant.getMeeting(), currentUser);

        // 상태 확인
        if (participant.getStatus() != MeetingParticipant.Status.PENDING) {
            throw new RuntimeException("승인 대기 중인 신청이 아닙니다");
        }

        // 거절 처리
        participant.reject(currentUser.getId(), request.getReason());
        MeetingParticipant savedParticipant = participantRepository.save(participant);

        return convertToParticipantResponse(savedParticipant);
    }

    /**
     * 모임별 참가자 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<MeetingParticipantDto.ParticipantSummary> getMeetingParticipants(Long meetingId,
                                                                                 MeetingParticipant.Status status,
                                                                                 int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));

        Page<MeetingParticipant> participants;
        if (status != null) {
            participants = participantRepository.findByMeetingIdAndStatus(meetingId, status, pageable);
        } else {
            participants = participantRepository.findByMeetingId(meetingId, pageable);
        }

        return participants.map(this::convertToParticipantSummary);
    }

    /**
     * 사용자별 참여 모임 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<MeetingParticipantDto.MyParticipationSummary> getMyParticipations(User currentUser,
                                                                                  MeetingParticipant.Status status,
                                                                                  int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));

        Page<MeetingParticipant> participants;
        if (status != null) {
            participants = participantRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable);
        } else {
            participants = participantRepository.findByUserId(currentUser.getId(), pageable);
        }

        return participants.map(this::convertToMyParticipationSummary);
    }

    /**
     * 주최자별 참여 신청 관리 목록
     */
    @Transactional(readOnly = true)
    public Page<MeetingParticipantDto.ApplicationSummary> getApplicationsForOrganizer(User organizer,
                                                                                      MeetingParticipant.Status status,
                                                                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "appliedAt"));

        Page<MeetingParticipant> applications;
        if (status != null) {
            applications = participantRepository.findPendingApplicationsByOrganizer(organizer.getId(), pageable);
        } else {
            applications = participantRepository.findApplicationsByOrganizer(organizer.getId(), pageable);
        }

        return applications.map(this::convertToApplicationSummary);
    }

    /**
     * 참여 상태 조회
     */
    @Transactional(readOnly = true)
    public MeetingParticipantDto.ParticipationStatus getParticipationStatus(Long meetingId, User currentUser) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, currentUser.getId())
                .map(this::convertToParticipationStatus)
                .orElse(createNonParticipatingStatus());
    }

    /**
     * 참여 통계 조회
     */
    @Transactional(readOnly = true)
    public MeetingParticipantDto.ParticipationStats getParticipationStats(Long meetingId) {
        List<Object[]> stats = participantRepository.getParticipantStatsByMeeting(meetingId);

        MeetingParticipantDto.ParticipationStats result = new MeetingParticipantDto.ParticipationStats();
        result.setMeetingId(meetingId);

        for (Object[] stat : stats) {
            MeetingParticipant.Status status = (MeetingParticipant.Status) stat[0];
            Long count = (Long) stat[1];

            switch (status) {
                case PENDING -> result.setPendingCount(count.intValue());
                case APPROVED -> result.setApprovedCount(count.intValue());
                case REJECTED -> result.setRejectedCount(count.intValue());
                case CANCELLED -> result.setCancelledCount(count.intValue());
            }
        }

        result.setTotalApplications(result.getPendingCount() + result.getApprovedCount() +
                result.getRejectedCount() + result.getCancelledCount());

        return result;
    }

    /**
     * 곧 시작되는 참여 모임 조회
     */
    @Transactional(readOnly = true)
    public List<MeetingParticipantDto.UpcomingMeetingSummary> getUpcomingMeetings(User currentUser) {
        List<MeetingParticipant> upcomingParticipants = participantRepository.findUserUpcomingMeetings(currentUser.getId());

        return upcomingParticipants.stream()
                .map(this::convertToUpcomingMeetingSummary)
                .collect(Collectors.toList());
    }

    // ==================== 헬퍼 메서드들 ====================

    /**
     * 참여 신청 유효성 검증
     */
    private void validateJoinRequest(Meeting meeting, User currentUser) {
        // 모임 상태 확인
        if (meeting.getStatus() != Meeting.Status.RECRUITING) {
            throw new RuntimeException("모집이 마감된 모임입니다");
        }

        // 모임 시작 시간 확인
        if (meeting.getStartDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new RuntimeException("모임 시작 1시간 전부터는 참여 신청할 수 없습니다");
        }

        // 정원 확인
        if (!meeting.canJoin()) {
            throw new RuntimeException("모임 정원이 가득 찼습니다");
        }

        // 자신이 주최한 모임인지 확인
        if (Objects.equals(meeting.getCreatedBy().getId(), currentUser.getId())) {
            throw new RuntimeException("자신이 주최한 모임에는 신청할 수 없습니다");
        }
    }

    /**
     * 주최자 권한 확인
     */
    private void validateOrganizerPermission(Meeting meeting, User currentUser) {
        if (!Objects.equals(meeting.getCreatedBy().getId(), currentUser.getId()) &&
                currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("모임 관리 권한이 없습니다");
        }
    }

    /**
     * 자동 승인 여부 확인
     */
    // 기존 isAutoApprovalEnabled 메서드를 설정값 기반으로 변경
    private boolean isAutoApprovalEnabled(Meeting meeting) {
        Long meetingId = meeting.getId();
        Integer limit = autoApprovalLimits.get(meetingId);

        if (limit == null || limit <= 0) {
            return false;  // 자동승인 설정 안 함
        }

        AtomicInteger currentCount = autoApprovalCounts.get(meetingId);
        if (currentCount == null) {
            return false;
        }

        return currentCount.get() < limit;  // 한도 내에서만 자동승인
    }
    /**
     * 모임 참가자 수 업데이트
     */
    private void updateMeetingParticipantCount(Long meetingId) {
        meetingRepository.updateParticipantCount(meetingId);
    }

    // ==================== DTO 변환 메서드들 ====================

    private MeetingParticipantDto.ParticipantResponse convertToParticipantResponse(MeetingParticipant participant) {
        MeetingParticipantDto.ParticipantResponse response = new MeetingParticipantDto.ParticipantResponse();
        response.setId(participant.getId());
        response.setStatus(participant.getStatus());
        response.setApplicationMessage(participant.getApplicationMessage());
        response.setAppliedAt(participant.getAppliedAt());
        response.setProcessedAt(participant.getProcessedAt());
        response.setRejectionReason(participant.getRejectionReason());

        // 모임 정보
        response.setMeetingId(participant.getMeeting().getId());
        response.setMeetingTitle(participant.getMeeting().getTitle());

        // 사용자 정보
        response.setUserId(participant.getUser().getId());
        response.setUserName(participant.getUser().getName());
        response.setUserProfileImageUrl(participant.getUser().getProfileImageUrl());

        return response;
    }

    private MeetingParticipantDto.ParticipantSummary convertToParticipantSummary(MeetingParticipant participant) {
        MeetingParticipantDto.ParticipantSummary summary = new MeetingParticipantDto.ParticipantSummary();
        summary.setId(participant.getId());
        summary.setUserId(participant.getUser().getId());
        summary.setUserName(participant.getUser().getName());
        summary.setUserProfileImageUrl(participant.getUser().getProfileImageUrl());
        summary.setStatus(participant.getStatus());
        summary.setAppliedAt(participant.getAppliedAt());
        summary.setApplicationMessage(participant.getApplicationMessage());

        return summary;
    }

    private MeetingParticipantDto.MyParticipationSummary convertToMyParticipationSummary(MeetingParticipant participant) {
        MeetingParticipantDto.MyParticipationSummary summary = new MeetingParticipantDto.MyParticipationSummary();
        summary.setId(participant.getId());
        summary.setStatus(participant.getStatus());
        summary.setAppliedAt(participant.getAppliedAt());
        summary.setProcessedAt(participant.getProcessedAt());

        // 모임 정보
        Meeting meeting = participant.getMeeting();
        summary.setMeetingId(meeting.getId());
        summary.setMeetingTitle(meeting.getTitle());
        summary.setMeetingCategory(meeting.getCategory());
        summary.setMeetingLocation(meeting.getLocation());
        summary.setMeetingStartDate(meeting.getStartDate());
        summary.setMeetingEndDate(meeting.getEndDate());
        summary.setMeetingStatus(meeting.getStatus());
        summary.setMeetingCurrentParticipants(meeting.getCurrentParticipants());
        summary.setMeetingMaxParticipants(meeting.getMaxParticipants());

        return summary;
    }

    private MeetingParticipantDto.ApplicationSummary convertToApplicationSummary(MeetingParticipant participant) {
        MeetingParticipantDto.ApplicationSummary summary = new MeetingParticipantDto.ApplicationSummary();
        summary.setId(participant.getId());
        summary.setApplicantId(participant.getUser().getId());
        summary.setApplicantName(participant.getUser().getName());
        summary.setApplicantProfileImageUrl(participant.getUser().getProfileImageUrl());
        summary.setApplicationMessage(participant.getApplicationMessage());
        summary.setStatus(participant.getStatus());
        summary.setAppliedAt(participant.getAppliedAt());
        summary.setProcessedAt(participant.getProcessedAt());
        summary.setRejectionReason(participant.getRejectionReason());

        // 모임 정보
        summary.setMeetingId(participant.getMeeting().getId());
        summary.setMeetingTitle(participant.getMeeting().getTitle());
        summary.setMeetingStartDate(participant.getMeeting().getStartDate());

        return summary;
    }

    private MeetingParticipantDto.ParticipationStatus convertToParticipationStatus(MeetingParticipant participant) {
        MeetingParticipantDto.ParticipationStatus status = new MeetingParticipantDto.ParticipationStatus();
        status.setIsParticipating(true);
        status.setApplicationStatus(participant.getStatus());
        status.setAppliedAt(participant.getAppliedAt());
        status.setProcessedAt(participant.getProcessedAt());
        status.setCanCancel(participant.canCancel());
        status.setCanJoinChat(participant.getStatus() == MeetingParticipant.Status.APPROVED);

        return status;
    }

    private MeetingParticipantDto.ParticipationStatus createNonParticipatingStatus() {
        MeetingParticipantDto.ParticipationStatus status = new MeetingParticipantDto.ParticipationStatus();
        status.setIsParticipating(false);
        status.setCanCancel(false);
        status.setCanJoinChat(false);

        return status;
    }

    private MeetingParticipantDto.UpcomingMeetingSummary convertToUpcomingMeetingSummary(MeetingParticipant participant) {
        MeetingParticipantDto.UpcomingMeetingSummary summary = new MeetingParticipantDto.UpcomingMeetingSummary();

        Meeting meeting = participant.getMeeting();
        summary.setMeetingId(meeting.getId());
        summary.setMeetingTitle(meeting.getTitle());
        summary.setMeetingLocation(meeting.getLocation());
        summary.setMeetingStartDate(meeting.getStartDate());
        summary.setMeetingEndDate(meeting.getEndDate());
        summary.setParticipationStatus(participant.getStatus());

        // D-Day 계산
        long daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                meeting.getStartDate().toLocalDate()
        );
        summary.setDaysUntilStart((int) daysUntilStart);

        return summary;
    }
}