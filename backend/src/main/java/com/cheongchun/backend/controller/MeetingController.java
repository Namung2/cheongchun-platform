package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.*;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.MeetingService;
import com.cheongchun.backend.util.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/meetings")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MeetingController {

    private static final Logger logger = LoggerFactory.getLogger(MeetingController.class);
    
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
            return ControllerUtils.createErrorResponse("MEETING_CREATION_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("INTERNAL_ERROR", "모임 생성 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
            return ControllerUtils.createErrorResponse("MEETING_NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("INTERNAL_ERROR", "모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
            // CreatingMeetingRequest.MeetingSearchRequest 객체 생성 및 null 값 필터링
            CreatingMeetingRequest.MeetingSearchRequest searchRequest = new CreatingMeetingRequest.MeetingSearchRequest();

            // null이 아닌 값만 설정 (PostgreSQL 타입 추론 오류 방지)
            if (keyword != null && !keyword.trim().isEmpty()) {
                searchRequest.setKeyword(keyword.trim());
            }

            searchRequest.setCategory(category); // enum은 null이어도 괜찮음

            if (subcategory != null && !subcategory.trim().isEmpty()) {
                searchRequest.setSubcategory(subcategory.trim());
            }

            if (location != null && !location.trim().isEmpty()) {
                searchRequest.setLocation(location.trim());
            }

            searchRequest.setMinFee(minFee);
            searchRequest.setMaxFee(maxFee);
            searchRequest.setDifficultyLevel(difficultyLevel); // enum은 null이어도 괜찼음

            // Date parsing validation needed if implemented

            searchRequest.setSortBy(sortBy != null ? sortBy : "LATEST");
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setStatus(Meeting.Status.RECRUITING); // 기본값 설정

            // 🔥 임시 해결책: 복잡한 필터가 있으면 단순 검색으로 우회
            Page<CreatingMeetingRequest.MeetingSummary> meetings;

            if (hasComplexFilters(keyword, category, subcategory, location, minFee, maxFee, difficultyLevel)) {
                // 복잡한 필터의 경우 단계별로 처리
                meetings = meetingService.getMeetingsSimple(searchRequest, currentUser);
            } else {
                // 기존 방식 사용
                meetings = meetingService.getMeetings(searchRequest, currentUser);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "모임 목록 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("모임 목록 조회 중 오류 발생", e);
            return ControllerUtils.createErrorResponse("MEETING_LIST_ERROR", "모임 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private boolean hasComplexFilters(String keyword, Meeting.Category category, String subcategory,
                                      String location, Integer minFee, Integer maxFee, Meeting.DifficultyLevel difficultyLevel) {
        int filterCount = 0;

        if (keyword != null && !keyword.trim().isEmpty()) filterCount++;
        if (category != null) filterCount++;
        if (subcategory != null && !subcategory.trim().isEmpty()) filterCount++;
        if (location != null && !location.trim().isEmpty()) filterCount++;
        if (minFee != null) filterCount++;
        if (maxFee != null) filterCount++;
        if (difficultyLevel != null) filterCount++;

        return filterCount >= 3; // 3개 이상이면 복잡한 검색
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
            return ControllerUtils.createErrorResponse("MEETING_UPDATE_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("INTERNAL_ERROR", "모임 수정 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
            return ControllerUtils.createErrorResponse("MEETING_DELETE_FAILED", e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("INTERNAL_ERROR", "모임 삭제 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "오늘의 베스트 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("TODAY_BEST_ERROR", "오늘의 베스트 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "추천 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("RECOMMENDATION_ERROR", "추천 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 카테고리별 모임 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getMeetingsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            // 카테고리 변환 및 검증
            Meeting.Category meetingCategory = parseCategory(category);

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMeetingsByCategory(meetingCategory, page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", ControllerUtils.createPaginationInfo(meetings),
                    "category", meetingCategory.name()
            ));
            response.put("message", meetingCategory.name() + " 카테고리 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ControllerUtils.createErrorResponse("INVALID_CATEGORY",
                    "유효하지 않은 카테고리입니다. 사용 가능한 카테고리: " + getValidCategories(),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("CATEGORY_ERROR", "카테고리별 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 카테고리 문자열을 enum으로 안전하게 변환
     */
    private Meeting.Category parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리가 제공되지 않았습니다");
        }

        // 중괄호 제거 (혹시 {CULTURE} 형태로 들어왔을 경우)
        String cleanCategory = categoryStr.replaceAll("[{}]", "").trim().toUpperCase();

        try {
            return Meeting.Category.valueOf(cleanCategory);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리: " + categoryStr +
                    ". 사용 가능한 카테고리: " + getValidCategories());
        }
    }

    /**
     * 유효한 카테고리 목록 반환
     */
    private String getValidCategories() {
        return java.util.Arrays.stream(Meeting.Category.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
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
                return ControllerUtils.createErrorResponse("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
            }

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMyMeetings(currentUser, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "내 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("MY_MEETINGS_ERROR", "내 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                return ControllerUtils.createErrorResponse("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
            }

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.getMyParticipatingMeetings(currentUser, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "참여 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("PARTICIPATING_MEETINGS_ERROR", "참여 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                return ControllerUtils.createErrorResponse("INVALID_KEYWORD", "검색 키워드를 입력해주세요", HttpStatus.BAD_REQUEST);
            }

            Page<CreatingMeetingRequest.MeetingSummary> meetings = meetingService.searchMeetings(keyword.trim(), page, size, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "meetings", meetings.getContent(),
                    "pagination", ControllerUtils.createPaginationInfo(meetings),
                    "keyword", keyword.trim()
            ));
            response.put("message", "모임 검색 완료");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("SEARCH_ERROR", "모임 검색 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "취미 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("HOBBY_MEETINGS_ERROR", "취미 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
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
                    "pagination", ControllerUtils.createPaginationInfo(meetings)
            ));
            response.put("message", "고민/사연 모임 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ControllerUtils.createErrorResponse("TALK_MEETINGS_ERROR", "고민/사연 모임 조회 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



}