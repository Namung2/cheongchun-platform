package com.cheongchun.backend.dto;

import com.cheongchun.backend.entity.Meeting;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class WishlistDto {

    /**
     * 찜 응답 (상세 정보)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistResponse {
        private Long id;
        private Long userId;
        private Long meetingId;
        private String meetingTitle;
        private Meeting.Category meetingCategory;
        private String meetingLocation;
        private LocalDateTime meetingStartDate;
        private Integer meetingFee;
        private Meeting.Status meetingStatus;
        private LocalDateTime createdAt;
    }

    /**
     * 찜 목록 요약 (목록용)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistSummary {
        private Long id;
        private LocalDateTime addedAt;

        // 모임 정보
        private Long meetingId;
        private String meetingTitle;
        private String meetingDescription;
        private Meeting.Category meetingCategory;
        private String meetingSubcategory;
        private String meetingLocation;
        private LocalDateTime meetingStartDate;
        private LocalDateTime meetingEndDate;
        private Integer meetingFee;
        private Integer meetingCurrentParticipants;
        private Integer meetingMaxParticipants;
        private Meeting.Status meetingStatus;
        private Integer meetingViewCount;
        private Integer meetingWishlistCount;
    }

    /**
     * 곧 시작되는 찜한 모임 요약
     */
    @Data
    @NoArgsConstructor
    public static class UpcomingWishlistSummary {
        private Long meetingId;
        private String meetingTitle;
        private String meetingLocation;
        private LocalDateTime meetingStartDate;
        private LocalDateTime meetingEndDate;
        private LocalDateTime addedAt;
        private Integer daysUntilStart;
    }

    /**
     * 찜 통계
     */
    @Data
    public static class WishlistStats {
        private Long userId;
        private Integer totalWishlistCount;
        private Integer hobbyCount;
        private Integer exerciseCount;
        private Integer cultureCount;
        private Integer educationCount;
        private Integer talkCount;
        private Integer volunteerCount;

        public WishlistStats() {
            this.totalWishlistCount = 0;
            this.hobbyCount = 0;
            this.exerciseCount = 0;
            this.cultureCount = 0;
            this.educationCount = 0;
            this.talkCount = 0;
            this.volunteerCount = 0;
        }
    }

    /**
     * 인기 모임 요약 (찜 기준)
     */
    @Data
    @NoArgsConstructor
    public static class PopularMeetingSummary {
        private Long meetingId;
        private String meetingTitle;
        private Meeting.Category meetingCategory;
        private String meetingLocation;
        private LocalDateTime meetingStartDate;
        private Integer meetingFee;
        private Integer wishlistCount;
        private Integer currentParticipants;
        private Integer maxParticipants;
    }

    /**
     * 찜 검색 요청
     */
    @Data
    @NoArgsConstructor
    public static class WishlistSearchRequest {
        private Meeting.Category category;
        private String location;
        private Meeting.Status status;
        private String sortBy = "LATEST"; // LATEST, OLDEST, MEETING_START_DATE, MEETING_POPULARITY
        private Integer page = 0;
        private Integer size = 20;
    }

    /**
     * 배치 찜 작업 요청
     */
    @Data
    @NoArgsConstructor
    public static class BatchWishlistRequest {
        @NotEmpty(message = "모임 ID 목록은 비어있을 수 없습니다")
        private List<Long> meetingIds;

        @NotNull(message = "작업 유형을 선택해주세요")
        private Action action;

        public enum Action {
            ADD, REMOVE
        }
    }

    /**
     * 배치 찜 작업 응답
     */
    @Data
    public static class BatchWishlistResponse {
        private Integer successCount;
        private Integer failureCount;
        private List<String> errors;

        public BatchWishlistResponse() {
            this.successCount = 0;
            this.failureCount = 0;
            this.errors = new java.util.ArrayList<>();
        }
    }

    /**
     * 찜 분석 요청 (AI 추천용)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistAnalysisRequest {
        private Long userId;
        private Integer recentDays = 30; // 최근 N일간의 찜 데이터 분석
        private Boolean includeCategories = true;
        private Boolean includeTimePatterns = true;
        private Boolean includeLocationPatterns = true;
    }

    /**
     * 찜 분석 응답 (AI 추천용)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistAnalysisResponse {
        private Long userId;
        private Integer totalWishlists;
        private List<CategoryPreference> categoryPreferences;
        private List<TimePreference> timePreferences;
        private List<LocationPreference> locationPreferences;
        private List<Long> frequentlyWishlistedMeetingTypes;

        @Data
        @NoArgsConstructor
        public static class CategoryPreference {
            private Meeting.Category category;
            private Integer count;
            private Double percentage;
        }

        @Data
        @NoArgsConstructor
        public static class TimePreference {
            private String timeSlot; // MORNING, AFTERNOON, EVENING
            private Integer count;
            private Double percentage;
        }

        @Data
        @NoArgsConstructor
        public static class LocationPreference {
            private String location;
            private Integer count;
            private Double percentage;
        }
    }

    /**
     * 찜 비교 요청 (모임 비교 기능과 연동)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistComparisonRequest {
        @NotEmpty(message = "비교할 모임을 선택해주세요")
        private List<Long> meetingIds;

        private String comparisonType = "BASIC"; // BASIC, DETAILED, AI_ANALYSIS
    }

    /**
     * 찜 추천 요청 (사용자의 찜 패턴 기반)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistBasedRecommendationRequest {
        private Integer limit = 10;
        private Boolean excludeWishlisted = true;
        private Boolean excludeParticipated = true;
        private Meeting.Category categoryFilter;
        private String locationFilter;
    }

    /**
     * 찜 목록 내보내기 요청
     */
    @Data
    @NoArgsConstructor
    public static class WishlistExportRequest {
        private String format = "JSON"; // JSON, CSV, EXCEL
        private Boolean includeExpiredMeetings = false;
        private Meeting.Category categoryFilter;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    /**
     * 찜 목록 가져오기 요청 (다른 플랫폼에서)
     */
    @Data
    @NoArgsConstructor
    public static class WishlistImportRequest {
        private String source; // FACEBOOK, MEETUP, EVENTBRITE 등
        private String accessToken;
        private Boolean mergeWithExisting = true;
    }
}