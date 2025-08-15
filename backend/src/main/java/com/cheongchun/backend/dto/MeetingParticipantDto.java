package com.cheongchun.backend.dto;

import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.MeetingParticipant;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MeetingParticipantDto {

    /**
     * 모임 참여 신청 요청
     */
    @Data
    @NoArgsConstructor
    public static class JoinRequest {
        @Size(max = 500, message = "신청 메시지는 500자를 초과할 수 없습니다")
        private String applicationMessage;
    }

    /**
     * 참여 신청 거절 요청
     */
    @Data
    @NoArgsConstructor
    public static class RejectRequest {
        @Size(max = 200, message = "거절 사유는 200자를 초과할 수 없습니다")
        private String reason;
    }

    /**
     * 참가자 응답 (상세 정보)
     */
    @Data
    @NoArgsConstructor
    public static class ParticipantResponse {
        private Long id;
        private Long meetingId;
        private String meetingTitle;
        private Long userId;
        private String userName;
        private String userProfileImageUrl;
        private MeetingParticipant.Status status;
        private String applicationMessage;
        private LocalDateTime appliedAt;
        private LocalDateTime processedAt;
        private String rejectionReason;
    }

    /**
     * 참가자 요약 정보 (목록용)
     */
    @Data
    @NoArgsConstructor
    public static class ParticipantSummary {
        private Long id;
        private Long userId;
        private String userName;
        private String userProfileImageUrl;
        private MeetingParticipant.Status status;
        private String applicationMessage;
        private LocalDateTime appliedAt;
    }

    /**
     * 내 참여 모임 요약
     */
    @Data
    @NoArgsConstructor
    public static class MyParticipationSummary {
        private Long id;
        private MeetingParticipant.Status status;
        private LocalDateTime appliedAt;
        private LocalDateTime processedAt;

        // 모임 정보
        private Long meetingId;
        private String meetingTitle;
        private Meeting.Category meetingCategory;
        private String meetingLocation;
        private LocalDateTime meetingStartDate;
        private LocalDateTime meetingEndDate;
        private Meeting.Status meetingStatus;
        private Integer meetingCurrentParticipants;
        private Integer meetingMaxParticipants;
    }

    /**
     * 신청 관리 요약 (주최자용)
     */
    @Data
    @NoArgsConstructor
    public static class ApplicationSummary {
        private Long id;
        private Long applicantId;
        private String applicantName;
        private String applicantProfileImageUrl;
        private String applicationMessage;
        private MeetingParticipant.Status status;
        private LocalDateTime appliedAt;
        private LocalDateTime processedAt;
        private String rejectionReason;

        // 모임 정보
        private Long meetingId;
        private String meetingTitle;
        private LocalDateTime meetingStartDate;
    }

    /**
     * 참여 상태 정보
     */
    @Data
    @NoArgsConstructor
    public static class ParticipationStatus {
        private Boolean isParticipating;
        private MeetingParticipant.Status applicationStatus;
        private LocalDateTime appliedAt;
        private LocalDateTime processedAt;
        private Boolean canCancel;
        private Boolean canJoinChat;
    }

    /**
     * 참여 통계
     */
    @Data
    public static class ParticipationStats {
        private Long meetingId;
        private Integer totalApplications;
        private Integer pendingCount;
        private Integer approvedCount;
        private Integer rejectedCount;
        private Integer cancelledCount;

        public ParticipationStats() {
            this.totalApplications = 0;
            this.pendingCount = 0;
            this.approvedCount = 0;
            this.rejectedCount = 0;
            this.cancelledCount = 0;
        }
    }

    /**
     * 곧 시작되는 모임 요약
     */
    @Data
    @NoArgsConstructor
    public static class UpcomingMeetingSummary {
        private Long meetingId;
        private String meetingTitle;
        private String meetingLocation;
        private LocalDateTime meetingStartDate;
        private LocalDateTime meetingEndDate;
        private MeetingParticipant.Status participationStatus;
        private Integer daysUntilStart;
    }

    /**
     * 참여 신청 검색 요청
     */
    @Data
    @NoArgsConstructor
    public static class ParticipantSearchRequest {
        private Long meetingId;
        private MeetingParticipant.Status status;
        private LocalDateTime appliedAfter;
        private LocalDateTime appliedBefore;
        private String sortBy = "LATEST"; // LATEST, OLDEST
        private Integer page = 0;
        private Integer size = 20;
    }

    /**
     * 배치 승인/거절 요청
     */
    @Data
    @NoArgsConstructor
    public static class BatchActionRequest {
        private java.util.List<Long> participantIds;
        private MeetingParticipant.Status action; // APPROVED, REJECTED
        private String reason; // 거절 시 사유
    }

    /**
     * 배치 작업 응답
     */
    @Data
    @NoArgsConstructor
    public static class BatchActionResponse {
        private Integer successCount;
        private Integer failureCount;
        private java.util.List<String> errors;
        private java.util.List<ParticipantResponse> processedParticipants;
    }
}