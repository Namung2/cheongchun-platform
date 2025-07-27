package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.*;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/meetings")  // /api는 context-path에서 처리
@CrossOrigin(origins = "*", maxAge = 3600)
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    /**
     * 모임 생성
     */
    @PostMapping
    public ResponseEntity<?> createMeeting(@Valid @RequestBody CreatingMeetingRequest request,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            CreatingMeetingRequest.MeetingResponse meeting = meetingService.createMeeting(request, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", meeting);
            response.put("message", "모임이 성공적으로 생성되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return createErrorResponse("MEETING_CREATION_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "모임 생성 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 상세 조회
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<?> getMeeting(@PathVariable Long meetingId,
                                        @AuthenticationPrincipal User currentUser) {
        try {
            CreatingMeetingRequest.MeetingResponse meeting = meetingService.getMeetingById(meetingId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", meeting);
            response.put("message", "모임 정보 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("MEETING_NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 목록 조회 (검색 및 필터링 포함)
     */
    @GetMapping
    public ResponseEntity<?> getMeetings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Meeting.Category category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minFee,
            @RequestParam(required = false) Integer maxFee,
            @RequestParam(required = false) Meeting.DifficultyLevel difficultyLevel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "LATEST") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            // CreatingMeetingRequest.MeetingSearchRequest 객체 생성
            CreatingMeetingRequest.MeetingSearchRequest searchRequest = new CreatingMeetingRequest.MeetingSearchRequest();
            searchRequest.setKeyword(keyword);
            searchRequest.setCategory(category);
            searchRequest.setSubcategory(subcategory);
            searchRequest.setLocation(location);
            searchRequest.setMinFee(minFee);
            searchRequest.setMaxFee(maxFee);
            searchRequest.setDifficultyLevel(difficultyLevel);
            // TODO: startDate, endDate 파싱 추가
            searchRequest.setSortBy(sortBy);
            searchRequest.setPage(page);
            searchRequest.setSize(size);

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMeetings(searchRequest, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "모임 목록 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("MEETING_LIST_ERROR", "모임 목록 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 수정
     */
    @PutMapping("/{meetingId}")
    public ResponseEntity<?> updateMeeting(@PathVariable Long meetingId,
                                           @Valid @RequestBody CreatingMeetingRequest.UpdateMeetingRequest request,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            CreatingMeetingRequest.MeetingResponse meeting = meetingService.updateMeeting(meetingId, request, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", meeting);
            response.put("message", "모임이 성공적으로 수정되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("MEETING_UPDATE_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "모임 수정 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 삭제
     */
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long meetingId,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            meetingService.deleteMeeting(meetingId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모임이 성공적으로 삭제되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return createErrorResponse("MEETING_DELETE_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("INTERNAL_ERROR", "모임 삭제 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 오늘의 베스트 모임
     */
    @GetMapping("/today-best")
    public ResponseEntity<?> getTodayBestMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getTodayBestMeetings(page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "오늘의 베스트 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("TODAY_BEST_ERROR", "오늘의 베스트 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 추천 모임
     */
    @GetMapping("/recommended")
    public ResponseEntity<?> getRecommendedMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getRecommendedMeetings(page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "추천 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("RECOMMENDATION_ERROR", "추천 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 카테고리별 모임 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getMeetingsByCategory(
            @PathVariable Meeting.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMeetingsByCategory(category, page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings),
                    "category", category.name()
            ));
            response.put("message", category.name() + " 카테고리 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("CATEGORY_ERROR", "카테고리별 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내가 생성한 모임 조회
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            if (currentUser == null) {
                return createErrorResponse("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
            }

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMyMeetings(currentUser, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "내 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("MY_MEETINGS_ERROR", "내 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내가 참여한 모임 조회
     */
    @GetMapping("/participating")
    public ResponseEntity<?> getMyParticipatingMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            if (currentUser == null) {
                return createErrorResponse("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
            }

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMyParticipatingMeetings(currentUser, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "참여 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("PARTICIPATING_MEETINGS_ERROR", "참여 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모임 검색 (키워드)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMeetings(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return createErrorResponse("INVALID_KEYWORD", "검색 키워드를 입력해주세요", HttpStatus.BAD_REQUEST);
            }

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.searchMeetings(keyword.trim(), page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings),
                    "keyword", keyword.trim()
            ));
            response.put("message", "모임 검색 완료");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("SEARCH_ERROR", "모임 검색 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 취미 모임 조회 (API 명세서 호환)
     */
    @GetMapping("/hobby")
    public ResponseEntity<?> getHobbyMeetings(
            @RequestParam(required = false) String subcategory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMeetingsByCategory(Meeting.Category.HOBBY, page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "취미 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("HOBBY_MEETINGS_ERROR", "취미 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 고민/사연 모임 조회 (API 명세서 호환)
     */
    @GetMapping("/talk")
    public ResponseEntity<?> getTalkMeetings(
            @RequestParam(required = false) String talkType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMeetingsByCategory(Meeting.Category.TALK, page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", createPaginationInfo(meetings)
            ));
            response.put("message", "고민/사연 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("TALK_MEETINGS_ERROR", "고민/사연 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                "details", "모임 관련 작업을 처리할 수 없습니다"
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

    /**
     * 인증된 사용자 확인
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 테스트용 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<?> testMeetingController() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "모임 컨트롤러가 정상 작동합니다");
        response.put("timestamp", LocalDateTime.now());
        response.put("availableEndpoints", Map.of(
                "POST", "/api/meetings - 모임 생성",
                "GET", "/api/meetings - 모임 목록",
                "GET", "/api/meetings/{id} - 모임 상세",
                "PUT", "/api/meetings/{id} - 모임 수정",
                "DELETE", "/api/meetings/{id} - 모임 삭제",
                "GET", "/api/meetings/today-best - 오늘의 베스트",
                "GET", "/api/meetings/recommended - 추천 모임",
                "GET", "/api/meetings/my - 내 모임",
                "GET", "/api/meetings/participating - 참여 모임",
                "GET", "/api/meetings/search?keyword= - 검색"
        ));

        return ResponseEntity.ok(response);
    }
}