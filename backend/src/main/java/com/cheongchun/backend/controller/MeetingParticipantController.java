package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.MeetingParticipantDto;
import com.cheongchun.backend.entity.MeetingParticipant;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.MeetingParticipantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/meetings")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MeetingParticipantController {

    private final MeetingParticipantService participantService;

    public MeetingParticipantController(MeetingParticipantService participantService) {
        this.participantService = participantService;
    }

    /**
     * 모임 참여 신청
     */
    @PostMapping("/{meetingId}/join")
    public ResponseEntity<?> joinMeeting(@PathVariable Long meetingId,
                                         @Valid @RequestBody MeetingParticipantDto.JoinRequest request,
                                         @AuthenticationPrincipal User currentUser) {
        try {
            MeetingParticipantDto.ParticipantResponse response = participantService.joinMeeting(meetingId, request, currentUser);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "모임 참여 신청이 완료되었습니다");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return createErrorResponse("JOIN_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "모임 참여 신청 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 참여 신청 취소
     */
    @DeleteMapping("/{meetingId}/join")
    public ResponseEntity<?> cancelApplication(@PathVariable Long meetingId,
                                               @AuthenticationPrincipal User currentUser) {
        try {
            participantService.cancelApplication(meetingId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모임 참여 신청이 취소되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("CANCEL_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "참여 신청 취소 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 참여 상태 조회
     */
    @GetMapping("/{meetingId}/participation")
    public ResponseEntity<?> getParticipationStatus(@PathVariable Long meetingId,
                                                    @AuthenticationPrincipal User currentUser) {
        try {
            MeetingParticipantDto.ParticipationStatus status = participantService.getParticipationStatus(meetingId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);
            response.put("message", "참여 상태 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("STATUS_ERROR", "참여 상태 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임별 참가자 목록 조회
     */
    @GetMapping("/{meetingId}/participants")
    public ResponseEntity<?> getMeetingParticipants(@PathVariable Long meetingId,
                                                    @RequestParam(required = false) MeetingParticipant.Status status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        try {
            Page<MeetingParticipantDto.ParticipantSummary> participants =
                    participantService.getMeetingParticipants(meetingId, status, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "participants", participants.getContent(),
                    "pagination", createPaginationInfo(participants)
            ));
            response.put("message", "참가자 목록 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("PARTICIPANTS_ERROR", "참가자 목록 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내 참여 모임 목록 조회
     */
    @GetMapping("/my-participations")
    public ResponseEntity<?> getMyParticipations(@RequestParam(required = false) MeetingParticipant.Status status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @AuthenticationPrincipal User currentUser) {
        try {
            Page<MeetingParticipantDto.MyParticipationSummary> participations =
                    participantService.getMyParticipations(currentUser, status, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "participations", participations.getContent(),
                    "pagination", createPaginationInfo(participations)
            ));
            response.put("message", "내 참여 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("MY_PARTICIPATIONS_ERROR", "내 참여 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 곧 시작되는 참여 모임 조회
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingMeetings(@AuthenticationPrincipal User currentUser) {
        try {
            List<MeetingParticipantDto.UpcomingMeetingSummary> upcomingMeetings =
                    participantService.getUpcomingMeetings(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", upcomingMeetings);
            response.put("message", "곧 시작되는 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("UPCOMING_ERROR", "곧 시작되는 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 참여 신청 승인 (주최자용)
     */
    @PostMapping("/participants/{participantId}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long participantId,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            MeetingParticipantDto.ParticipantResponse response =
                    participantService.approveApplication(participantId, currentUser);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "참여 신청이 승인되었습니다");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return createErrorResponse("APPROVAL_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "신청 승인 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 참여 신청 거절 (주최자용)
     */
    @PostMapping("/participants/{participantId}/reject")
    public ResponseEntity<?> rejectApplication(@PathVariable Long participantId,
                                               @Valid @RequestBody MeetingParticipantDto.RejectRequest request,
                                               @AuthenticationPrincipal User currentUser) {
        try {
            MeetingParticipantDto.ParticipantResponse response =
                    participantService.rejectApplication(participantId, request, currentUser);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "참여 신청이 거절되었습니다");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return createErrorResponse("REJECTION_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "신청 거절 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 주최자별 참여 신청 관리 목록
     */
    @GetMapping("/my-applications")
    public ResponseEntity<?> getApplicationsForOrganizer(@RequestParam(required = false) MeetingParticipant.Status status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @AuthenticationPrincipal User currentUser) {
        try {
            Page<MeetingParticipantDto.ApplicationSummary> applications =
                    participantService.getApplicationsForOrganizer(currentUser, status, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "applications", applications.getContent(),
                    "pagination", createPaginationInfo(applications)
            ));
            response.put("message", "신청 관리 목록 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("APPLICATIONS_ERROR", "신청 관리 목록 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임별 참여 통계 조회
     */
    @GetMapping("/{meetingId}/participation-stats")
    public ResponseEntity<?> getParticipationStats(@PathVariable Long meetingId) {
        try {
            MeetingParticipantDto.ParticipationStats stats =
                    participantService.getParticipationStats(meetingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "참여 통계 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("STATS_ERROR", "참여 통계 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 배치 승인/거절 (주최자용)
     */
    @PostMapping("/participants/batch-action")
    public ResponseEntity<?> batchAction(@Valid @RequestBody MeetingParticipantDto.BatchActionRequest request,
                                         @AuthenticationPrincipal User currentUser) {
        try {
            MeetingParticipantDto.BatchActionResponse response = new MeetingParticipantDto.BatchActionResponse();
            response.setSuccessCount(0);
            response.setFailureCount(0);
            response.setErrors(new java.util.ArrayList<>());
            response.setProcessedParticipants(new java.util.ArrayList<>());

            // 배치 처리 로직
            for (Long participantId : request.getParticipantIds()) {
                try {
                    MeetingParticipantDto.ParticipantResponse participantResponse;

                    if (request.getAction() == MeetingParticipant.Status.APPROVED) {
                        participantResponse = participantService.approveApplication(participantId, currentUser);
                    } else if (request.getAction() == MeetingParticipant.Status.REJECTED) {
                        MeetingParticipantDto.RejectRequest rejectRequest = new MeetingParticipantDto.RejectRequest();
                        rejectRequest.setReason(request.getReason());
                        participantResponse = participantService.rejectApplication(participantId, rejectRequest, currentUser);
                    } else {
                        throw new RuntimeException("지원하지 않는 액션입니다: " + request.getAction());
                    }

                    response.getProcessedParticipants().add(participantResponse);
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } catch (Exception e) {
                    response.setFailureCount(response.getFailureCount() + 1);
                    response.getErrors().add("참가자 ID " + participantId + ": " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "배치 작업이 완료되었습니다");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return createErrorResponse("BATCH_ERROR", "배치 작업 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===================== 유틸리티 메서드들 =====================

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", Map.of(
                "code", code,
                "message", message,
                "details", "모임 참여 관련 작업을 처리할 수 없습니다"
        ));
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 페이지네이션 정보 생성
     */
    private Map<String, Object> createPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getNumber());
        pagination.put("size", page.getSize());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        pagination.put("isFirst", page.isFirst());
        pagination.put("isLast", page.isLast());
        return pagination;
    }
}