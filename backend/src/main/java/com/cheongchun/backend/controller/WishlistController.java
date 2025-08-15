package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.WishlistDto;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wishlist")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * 찜 추가
     */
    @PostMapping("/meetings/{meetingId}")
    public ResponseEntity<?> addToWishlist(@PathVariable Long meetingId,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            WishlistDto.WishlistResponse response = wishlistService.addToWishlist(meetingId, currentUser);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "모임이 찜 목록에 추가되었습니다");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return createErrorResponse("ADD_WISHLIST_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "찜 추가 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 찜 삭제
     */
    @DeleteMapping("/meetings/{meetingId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long meetingId,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            wishlistService.removeFromWishlist(meetingId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모임이 찜 목록에서 제거되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("REMOVE_WISHLIST_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "찜 삭제 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내 찜 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyWishlist(@RequestParam(required = false) Meeting.Category category,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) Meeting.Status status,
                                           @RequestParam(defaultValue = "LATEST") String sortBy,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            WishlistDto.WishlistSearchRequest searchRequest = new WishlistDto.WishlistSearchRequest();
            searchRequest.setCategory(category);
            searchRequest.setLocation(location);
            searchRequest.setStatus(status);
            searchRequest.setSortBy(sortBy);
            searchRequest.setPage(page);
            searchRequest.setSize(size);

            Page<WishlistDto.WishlistSummary> wishlists = wishlistService.getMyWishlist(currentUser, searchRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "wishlists", wishlists.getContent(),
                    "pagination", createPaginationInfo(wishlists)
            ));
            response.put("message", "찜 목록 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("WISHLIST_ERROR", "찜 목록 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 찜 여부 확인
     */
    @GetMapping("/meetings/{meetingId}/check")
    public ResponseEntity<?> checkWishlistStatus(@PathVariable Long meetingId,
                                                 @AuthenticationPrincipal User currentUser) {
        try {
            Boolean isWishlisted = wishlistService.isWishlisted(meetingId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetingId", meetingId,
                    "isWishlisted", isWishlisted
            ));
            response.put("message", "찜 상태 확인 완료");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("CHECK_ERROR", "찜 상태 확인 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 곧 시작되는 찜한 모임 조회
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingWishlistedMeetings(@AuthenticationPrincipal User currentUser) {
        try {
            List<WishlistDto.UpcomingWishlistSummary> upcomingMeetings =
                    wishlistService.getUpcomingWishlistedMeetings(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", upcomingMeetings);
            response.put("message", "곧 시작되는 찜한 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("UPCOMING_ERROR", "곧 시작되는 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 찜 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getWishlistStats(@AuthenticationPrincipal User currentUser) {
        try {
            WishlistDto.WishlistStats stats = wishlistService.getWishlistStats(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "찜 통계 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("STATS_ERROR", "찜 통계 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 찜했지만 아직 신청하지 않은 모임
     */
    @GetMapping("/not-applied")
    public ResponseEntity<?> getWishlistedButNotApplied(@AuthenticationPrincipal User currentUser) {
        try {
            List<WishlistDto.WishlistSummary> meetings =
                    wishlistService.getWishlistedButNotApplied(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", meetings);
            response.put("message", "신청하지 않은 찜 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("NOT_APPLIED_ERROR", "신청하지 않은 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 무료 찜 모임 조회
     */
    @GetMapping("/free")
    public ResponseEntity<?> getFreeWishlistedMeetings(@AuthenticationPrincipal User currentUser) {
        try {
            List<WishlistDto.WishlistSummary> freeMeetings =
                    wishlistService.getFreeWishlistedMeetings(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", freeMeetings);
            response.put("message", "무료 찜 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("FREE_MEETINGS_ERROR", "무료 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 인기 모임 (찜이 많은 순)
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularMeetingsByWishlist(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        try {
            Page<WishlistDto.PopularMeetingSummary> popularMeetings =
                    wishlistService.getPopularMeetingsByWishlist(page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", popularMeetings.getContent(),
                    "pagination", createPaginationInfo(popularMeetings)
            ));
            response.put("message", "인기 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("POPULAR_ERROR", "인기 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 기간에 찜한 모임들
     */
    @GetMapping("/period")
    public ResponseEntity<?> getWishlistBetweenDates(@RequestParam String startDate,
                                                     @RequestParam String endDate,
                                                     @AuthenticationPrincipal User currentUser) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime start = LocalDateTime.parse(startDate + " 00:00:00", formatter);
            LocalDateTime end = LocalDateTime.parse(endDate + " 23:59:59", formatter);

            List<WishlistDto.WishlistSummary> meetings =
                    wishlistService.getWishlistBetweenDates(currentUser, start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", meetings);
            response.put("message", "기간별 찜 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("PERIOD_ERROR", "기간별 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 배치 찜 추가/삭제
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchWishlistAction(@Valid @RequestBody WishlistDto.BatchWishlistRequest request,
                                                 @AuthenticationPrincipal User currentUser) {
        try {
            WishlistDto.BatchWishlistResponse response =
                    wishlistService.batchWishlistAction(request, currentUser);

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

    /**
     * 찜한 모임 ID 목록 조회 (AI 추천용)
     */
    @GetMapping("/meeting-ids")
    public ResponseEntity<?> getWishlistedMeetingIds(@AuthenticationPrincipal User currentUser) {
        try {
            List<Long> meetingIds = wishlistService.getWishlistedMeetingIds(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetingIds", meetingIds,
                    "count", meetingIds.size()
            ));
            response.put("message", "찜한 모임 ID 목록 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("MEETING_IDS_ERROR", "모임 ID 목록 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===================== 모임별 찜 관련 API (별칭 API - 호환성 위해) =====================

    /**
     * 모임별 찜 추가 (별칭 API)
     */
    @PostMapping("/{meetingId}")
    public ResponseEntity<?> addMeetingToWishlist(@PathVariable Long meetingId,
                                                  @AuthenticationPrincipal User currentUser) {
        return addToWishlist(meetingId, currentUser);
    }

    /**
     * 모임별 찜 삭제 (별칭 API)
     */
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<?> removeMeetingFromWishlist(@PathVariable Long meetingId,
                                                       @AuthenticationPrincipal User currentUser) {
        return removeFromWishlist(meetingId, currentUser);
    }

    /**
     * 찜 여부 확인 (별칭 API)
     */
    @GetMapping("/{meetingId}/check")
    public ResponseEntity<?> checkMeetingWishlistStatus(@PathVariable Long meetingId,
                                                        @AuthenticationPrincipal User currentUser) {
        return checkWishlistStatus(meetingId, currentUser);
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
                "details", "찜 관련 작업을 처리할 수 없습니다"
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