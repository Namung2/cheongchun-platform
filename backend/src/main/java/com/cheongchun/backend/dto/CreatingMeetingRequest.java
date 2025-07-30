package com.cheongchun.backend.dto;

import com.cheongchun.backend.entity.Meeting;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class CreatingMeetingRequest {

    @NotBlank(message = "모임 제목은 필수입니다")
    @Size(max = 200, message = "모임 제목은 200자를 초과할 수 없습니다")
    private String title;

    @Size(max = 5000, message = "모임 설명은 5000자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "카테고리는 필수입니다")
    private Meeting.Category category;

    @Size(max = 50, message = "서브카테고리는 50자를 초과할 수 없습니다")
    private String subcategory;

    @Size(max = 200, message = "지역은 200자를 초과할 수 없습니다")
    private String location;

    @Size(max = 500, message = "상세 주소는 500자를 초과할 수 없습니다")
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotNull(message = "시작 날짜는 필수입니다")
    @Future(message = "시작 날짜는 현재 시간 이후여야 합니다")
    private LocalDateTime startDate;

    @NotNull(message = "종료 날짜는 필수입니다")
    private LocalDateTime endDate;

    @Min(value = 1, message = "최대 참가자 수는 1명 이상이어야 합니다")
    @Max(value = 1000, message = "최대 참가자 수는 1000명을 초과할 수 없습니다")
    private Integer maxParticipants;

    @Min(value = 0, message = "참가비는 0원 이상이어야 합니다")
    private Integer fee = 0;

    private Meeting.DifficultyLevel difficultyLevel = Meeting.DifficultyLevel.BEGINNER;

    @Size(max = 50, message = "연령 범위는 50자를 초과할 수 없습니다")
    private String ageRange;

    @Size(max = 100, message = "주최자 연락처는 100자를 초과할 수 없습니다")
    private String organizerContact;

    @Size(max = 1000, message = "준비물은 1000자를 초과할 수 없습니다")
    private String preparationNeeded;

    @Size(max = 1000, message = "모임 규칙은 1000자를 초과할 수 없습니다")
    private String meetingRules;

    @JsonProperty("autoApprovalLimit")
    @Min(value = 0, message = "자동승인 한도는 0 이상이어야 합니다")
    @Max(value = 50, message = "자동승인 한도는 50 이하여야 합니다")
    private Integer autoApprovalLimit = 0;

    @Data
    @NoArgsConstructor
    public static class MeetingSummary {
        private Long id;
        private String title;
        private String description;
        private Meeting.Category category;
        private String subcategory;
        private String location;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private Integer fee;
        private Meeting.DifficultyLevel difficultyLevel;
        private String ageRange;
        private Integer viewCount;
        private Integer wishlistCount;
        private Double averageRating;
        private Meeting.Status status;
        private Boolean isWishlisted;
        private Boolean isParticipating;
        private Double distance;
        private String mainImageUrl;
        private CreatingMeetingRequest.MeetingResponse.UserSummary createdBy;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class MeetingSearchRequest {
        private String keyword;
        private Meeting.Category category;
        private String subcategory;
        private String location;
        private Integer minFee;
        private Integer maxFee;
        private Meeting.DifficultyLevel difficultyLevel;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Meeting.Status status = Meeting.Status.RECRUITING;
        private String sortBy = "LATEST"; // LATEST, POPULAR, NEAREST, RATING, START_DATE
        private Integer page = 0;
        private Integer size = 20;
    }

    @Data
    @NoArgsConstructor
    public static class JoinMeetingRequest {
        @Size(max = 500, message = "신청 메시지는 500자를 초과할 수 없습니다")
        private String applicationMessage;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateMeetingRequest {

        @Size(max = 200, message = "모임 제목은 200자를 초과할 수 없습니다")
        private String title;

        @Size(max = 5000, message = "모임 설명은 5000자를 초과할 수 없습니다")
        private String description;

        private Meeting.Category category;

        @Size(max = 50, message = "서브카테고리는 50자를 초과할 수 없습니다")
        private String subcategory;

        @Size(max = 200, message = "지역은 200자를 초과할 수 없습니다")
        private String location;

        @Size(max = 500, message = "상세 주소는 500자를 초과할 수 없습니다")
        private String address;

        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        @Min(value = 1, message = "최대 참가자 수는 1명 이상이어야 합니다")
        private Integer maxParticipants;

        @Min(value = 0, message = "참가비는 0원 이상이어야 합니다")
        private Integer fee;

        private Meeting.DifficultyLevel difficultyLevel;

        @Size(max = 50, message = "연령 범위는 50자를 초과할 수 없습니다")
        private String ageRange;

        @Size(max = 100, message = "주최자 연락처는 100자를 초과할 수 없습니다")
        private String organizerContact;

        @Size(max = 1000, message = "준비물은 1000자를 초과할 수 없습니다")
        private String preparationNeeded;

        @Size(max = 1000, message = "모임 규칙은 1000자를 초과할 수 없습니다")
        private String meetingRules;
    }

    @Data
    @NoArgsConstructor
    public static class MeetingResponse {

        private Long id;
        private String title;
        private String description;
        private Meeting.Category category;
        private String subcategory;
        private String location;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private Integer fee;
        private Meeting.DifficultyLevel difficultyLevel;
        private String ageRange;
        private Meeting.MeetingType meetingType;
        private String source;
        private String organizerContact;
        private String preparationNeeded;
        private String meetingRules;
        private Integer viewCount;
        private Integer dailyViewCount;
        private Integer wishlistCount;
        private Integer shareCount;
        private Double averageRating;
        private Integer reviewCount;
        private Meeting.Status status;
        private Boolean isWishlisted;
        private Boolean isParticipating;
        private String participationStatus;
        private Double distance;
        private UserSummary createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ParticipantSummary> participants;
        private List<String> images;

        @Data
        @NoArgsConstructor
        public static class UserSummary {
            private Long id;
            private String name;
            private String profileImageUrl;
        }

        @Data
        @NoArgsConstructor
        public static class ParticipantSummary {
            private Long id;
            private String name;
            private String profileImageUrl;
            private Integer age;
            private String location;
        }
    }
}