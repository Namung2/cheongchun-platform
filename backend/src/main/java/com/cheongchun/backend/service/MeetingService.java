package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.*;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.MeetingParticipant;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.entity.UserWishlist;
import com.cheongchun.backend.repository.MeetingParticipantRepository;
import com.cheongchun.backend.repository.MeetingRepository;
import com.cheongchun.backend.repository.UserWishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MeetingService {

    @Autowired
    private final MeetingRepository meetingRepository;
    @Autowired
    private final MeetingParticipantRepository participantRepository;
    @Autowired
    private final UserWishlistRepository wishlistRepository;

    public MeetingService(MeetingRepository meetingRepository,
                          MeetingParticipantRepository participantRepository,
                          UserWishlistRepository wishlistRepository) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.wishlistRepository = wishlistRepository;
    }

    /**
     * 모임 생성
     */
    public CreatingMeetingRequest.MeetingResponse createMeeting(CreatingMeetingRequest request, User currentUser) {
        // 비즈니스 규칙 검증
        validateMeetingRequest(request);

        // 중복 모임 확인 (같은 사용자, 같은 제목, 같은 시작 시간)
        if (meetingRepository.existsByTitleAndCreatedByAndStartDate(
                request.getTitle(), currentUser, request.getStartDate())) {
            throw new RuntimeException("이미 같은 시간에 동일한 제목의 모임이 존재합니다");
        }

        // Entity 생성
        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setCategory(request.getCategory());
        meeting.setSubcategory(request.getSubcategory());
        meeting.setLocation(request.getLocation());
        meeting.setAddress(request.getAddress());
        meeting.setLatitude(request.getLatitude());
        meeting.setLongitude(request.getLongitude());
        meeting.setStartDate(request.getStartDate());
        meeting.setEndDate(request.getEndDate());
        meeting.setMaxParticipants(request.getMaxParticipants());
        meeting.setFee(request.getFee());
        meeting.setDifficultyLevel(request.getDifficultyLevel());
        meeting.setAgeRange(request.getAgeRange());
        meeting.setOrganizerContact(request.getOrganizerContact());
        meeting.setPreparationNeeded(request.getPreparationNeeded());
        meeting.setMeetingRules(request.getMeetingRules());
        meeting.setCreatedBy(currentUser);
        meeting.setMeetingType(Meeting.MeetingType.USER_CREATED);
        meeting.setStatus(Meeting.Status.RECRUITING);

        Meeting savedMeeting = meetingRepository.save(meeting);

        // 주최자 자동 참여 처리
        autoJoinOrganizer(savedMeeting, currentUser);

        return convertToMeetingResponse(savedMeeting, currentUser);
    }

    /**
     * 모임 상세 조회
     */
    @Transactional(readOnly = true)
    public CreatingMeetingRequest.MeetingResponse getMeetingById(Long meetingId, User currentUser) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("모임을 찾을 수 없습니다"));

        // 조회수 증가 (비동기 처리 권장)
        incrementViewCount(meetingId);

        return convertToMeetingResponse(meeting, currentUser);
    }
    //모인 단순 조회
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getMeetingsSimple(
            CreatingMeetingRequest.MeetingSearchRequest searchRequest, User currentUser) {

        Pageable pageable = createPageable(searchRequest);
        Page<Meeting> meetings;

        // 키워드 검색 우선
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            meetings = meetingRepository.searchByKeyword(
                    searchRequest.getKeyword(),
                    searchRequest.getStatus(),
                    pageable
            );
        }
        // 카테고리 검색
        else if (searchRequest.getCategory() != null) {
            meetings = meetingRepository.findByCategoryAndStatus(
                    searchRequest.getCategory(),
                    searchRequest.getStatus(),
                    pageable
            );
        }
        // 지역 검색
        else if (searchRequest.getLocation() != null && !searchRequest.getLocation().trim().isEmpty()) {
            meetings = meetingRepository.findByLocationAndStatus(
                    searchRequest.getLocation(),
                    searchRequest.getStatus(),
                    pageable
            );
        }
        // 최대 참가비 검색
        else if (searchRequest.getMaxFee() != null) {
            meetings = meetingRepository.findByMaxFee(
                    searchRequest.getMaxFee(),
                    searchRequest.getStatus(),
                    pageable
            );
        }
        // 난이도 검색
        else if (searchRequest.getDifficultyLevel() != null) {
            meetings = meetingRepository.findByDifficultyLevelAndStatus(
                    searchRequest.getDifficultyLevel(),
                    searchRequest.getStatus(),
                    pageable
            );
        }
        // 기본: 전체 모임
        else {
            meetings = meetingRepository.findByStatus(searchRequest.getStatus(), pageable);
        }

        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 모임 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getMeetings(CreatingMeetingRequest.MeetingSearchRequest searchRequest, User currentUser) {
        Pageable pageable = createPageable(searchRequest);
        Page<Meeting> meetings;

        if (hasSearchFilters(searchRequest)) {
            meetings = searchMeetingsWithFilters(searchRequest, pageable);
        } else {
            meetings = meetingRepository.findByStatus(Meeting.Status.RECRUITING, pageable);
        }

        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 모임 수정
     */
    public CreatingMeetingRequest.MeetingResponse updateMeeting(Long meetingId, CreatingMeetingRequest.UpdateMeetingRequest request, User currentUser) {
        Meeting meeting = findMeetingWithAuthorization(meetingId, currentUser, "수정");

        // 모임 시작 2시간 전까지만 수정 가능
        if (meeting.getStartDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new RuntimeException("모임 시작 2시간 전부터는 수정할 수 없습니다");
        }

        // 필드 업데이트 (null이 아닌 값만)
        updateMeetingFields(meeting, request);

        Meeting updatedMeeting = meetingRepository.save(meeting);
        return convertToMeetingResponse(updatedMeeting, currentUser);
    }

    /**
     * 모임 삭제 (상태 변경)
     */
    public void deleteMeeting(Long meetingId, User currentUser) {
        Meeting meeting = findMeetingWithAuthorization(meetingId, currentUser, "삭제");

        // 참가자가 있으면 삭제 불가
        long participantCount = participantRepository.countApprovedParticipants(meetingId);
        if (participantCount > 1) { // 주최자 제외하고 참가자가 있으면
            throw new RuntimeException("참가자가 있는 모임은 삭제할 수 없습니다. 모임을 취소해주세요.");
        }

        meeting.setStatus(Meeting.Status.CANCELLED);
        meetingRepository.save(meeting);
    }

    /**
     * 오늘의 베스트 모임
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getTodayBestMeetings(int page, int size, User currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Meeting> meetings = meetingRepository.findTodayBestMeetings(Meeting.Status.RECRUITING, pageable);
        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 추천 모임 (기본 추천 - AI 전 단계)
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getRecommendedMeetings(int page, int size, User currentUser) {
        Pageable pageable = PageRequest.of(page, size);

        // 사용자의 관심 카테고리 기반 간단한 추천
        // TODO: 나중에 AI 서비스로 교체
        Page<Meeting> meetings = meetingRepository.findLatestMeetings(Meeting.Status.RECRUITING, pageable);
        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 카테고리별 모임 조회
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getMeetingsByCategory(Meeting.Category category, int page, int size, User currentUser) {
        Pageable pageable = createDefaultPageable(page, size);
        Page<Meeting> meetings = meetingRepository.findByCategoryAndStatus(category, Meeting.Status.RECRUITING, pageable);
        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 내가 생성한 모임 조회
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getMyMeetings(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Meeting> meetings = meetingRepository.findByCreatedByOrderByCreatedAtDesc(currentUser, pageable);
        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 내가 참여한 모임 조회
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> getMyParticipatingMeetings(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Meeting> meetings = meetingRepository.findMeetingsByParticipantUserId(currentUser.getId(), pageable);
        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    /**
     * 모임 검색
     */
    @Transactional(readOnly = true)
    public Page<CreatingMeetingRequest.MeetingSummary> searchMeetings(String keyword, int page, int size, User currentUser) {
        Pageable pageable = createDefaultPageable(page, size);
        Page<Meeting> meetings = meetingRepository.searchByKeyword(keyword, Meeting.Status.RECRUITING, pageable);
        return convertMeetingsToSummaryPage(meetings, currentUser);
    }

    // ===================== 공통 헬퍼 메서드들 =====================

    /**
     * Page<Meeting>을 Page<MeetingSummary>로 변환
     */
    private Page<CreatingMeetingRequest.MeetingSummary> convertMeetingsToSummaryPage(Page<Meeting> meetings, User currentUser) {
        return meetings.map(meeting -> convertToMeetingSummary(meeting, currentUser));
    }


    /**
     * 모임 조회 및 권한 확인 (개선된 버전)
     */
    private Meeting findMeetingWithAuthorization(Long meetingId, User currentUser, String action) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("모임을 찾을 수 없습니다"));

        // 권한 확인: ID 기반으로 비교 (더 안전한 방법)
        if (meeting.getCreatedBy() == null) {
            throw new RuntimeException("모임 주최자 정보가 없습니다");
        }

        if (currentUser == null) {
            throw new RuntimeException("인증된 사용자가 아닙니다");
        }

        if (!Objects.equals(meeting.getCreatedBy().getId(), currentUser.getId())) {
            throw new RuntimeException("모임을 " + action + "할 권한이 없습니다");
        }

        return meeting;
    }

    /**
     * 관리자 권한 확인 (추가 메서드)
     */
    private boolean isAdminOrOwner(Meeting meeting, User currentUser) {
        if (currentUser == null) {
            return false;
        }

        // 관리자이거나 모임 주최자인 경우
        return currentUser.getRole() == User.Role.ADMIN ||
                Objects.equals(meeting.getCreatedBy().getId(), currentUser.getId());
    }

    /**
     * 모임 권한 확인 (읽기 전용 - 조회용)
     */
    private boolean canViewMeeting(Meeting meeting, User currentUser) {
        // 모든 사용자가 모임을 조회할 수 있음 (공개된 정보)
        return true;
    }

    /**
     * 모임 수정 권한 확인
     */
    private boolean canEditMeeting(Meeting meeting, User currentUser) {
        if (currentUser == null) {
            return false;
        }

        // 관리자이거나 모임 주최자만 수정 가능
        return currentUser.getRole() == User.Role.ADMIN ||
                Objects.equals(meeting.getCreatedBy().getId(), currentUser.getId());
    }

    /**
     * 모임 삭제 권한 확인
     */
    private boolean canDeleteMeeting(Meeting meeting, User currentUser) {
        if (currentUser == null) {
            return false;
        }

        // 관리자이거나 모임 주최자만 삭제 가능
        return currentUser.getRole() == User.Role.ADMIN ||
                Objects.equals(meeting.getCreatedBy().getId(), currentUser.getId());
    }


    /**
     * 기본 페이지네이션 생성 (시간 역순)
     */
    private Pageable createDefaultPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * 주최자 정보 설정
     */
    private CreatingMeetingRequest.MeetingResponse.UserSummary createUserSummary(User user) {
        if (user == null) {
            return null;
        }
        CreatingMeetingRequest.MeetingResponse.UserSummary userSummary = new CreatingMeetingRequest.MeetingResponse.UserSummary();
        userSummary.setId(user.getId());
        userSummary.setName(user.getName());
        userSummary.setProfileImageUrl(user.getProfileImageUrl());
        return userSummary;
    }

    /**
     * 사용자별 상태 정보 설정 (MeetingResponse용)
     */
    private void setUserContextForResponse(CreatingMeetingRequest.MeetingResponse response, Long meetingId, User currentUser) {
        if (currentUser != null) {
            response.setIsWishlisted(isWishlisted(meetingId, currentUser.getId()));
            response.setIsParticipating(isParticipating(meetingId, currentUser.getId()));
            response.setParticipationStatus(getParticipationStatus(meetingId, currentUser.getId()));
        }
    }

    /**
     * 사용자별 상태 정보 설정 (MeetingSummary용)
     */
    private void setUserContextForSummary(CreatingMeetingRequest.MeetingSummary summary, Long meetingId, User currentUser) {
        if (currentUser != null) {
            summary.setIsWishlisted(isWishlisted(meetingId, currentUser.getId()));
            summary.setIsParticipating(isParticipating(meetingId, currentUser.getId()));
        }
    }

    // ===================== 내부 메서드들 =====================

    /**
     * 모임 요청 유효성 검증
     */
    private void validateMeetingRequest(CreatingMeetingRequest request) {
        // 시작일이 현재 시간 이후인지 확인
        if (request.getStartDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new RuntimeException("모임 시작 시간은 현재 시간으로부터 최소 1시간 이후여야 합니다");
        }

        // 종료일이 시작일 이후인지 확인
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("모임 종료 시간은 시작 시간 이후여야 합니다");
        }

        // 모임 시간이 너무 길지 않은지 확인 (최대 12시간)
        if (request.getEndDate().isAfter(request.getStartDate().plusHours(12))) {
            throw new RuntimeException("모임 시간은 최대 12시간을 초과할 수 없습니다");
        }
    }

    /**
     * 주최자 자동 참여 처리
     */
    private void autoJoinOrganizer(Meeting meeting, User organizer) {
        MeetingParticipant organizerParticipant = new MeetingParticipant();
        organizerParticipant.setMeeting(meeting);
        organizerParticipant.setUser(organizer);
        organizerParticipant.setStatus(MeetingParticipant.Status.APPROVED);
        organizerParticipant.setApplicationMessage("모임 주최자");

        participantRepository.save(organizerParticipant);

        // 참가자 수 업데이트
        meeting.incrementParticipants();
        meetingRepository.save(meeting);
    }

    /**
     * 조회수 증가 (비동기 처리 권장)
     */
    private void incrementViewCount(Long meetingId) {
        try {
            meetingRepository.incrementViewCount(meetingId);
        } catch (Exception e) {
            // 조회수 증가 실패는 전체 로직에 영향주지 않음
            System.err.println("조회수 증가 실패: " + e.getMessage());
        }
    }

    /**
     * 페이지 설정 생성
     */
    private Pageable createPageable(CreatingMeetingRequest.MeetingSearchRequest searchRequest) {
        Sort sort = createSort(searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    /**
     * 정렬 설정 생성
     */
    private Sort createSort(String sortBy) {
        return switch (sortBy.toUpperCase()) {
            case "POPULAR" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "WISHLIST" -> Sort.by(Sort.Direction.DESC, "wishlistCount");
            case "START_DATE" -> Sort.by(Sort.Direction.ASC, "startDate");
            case "PARTICIPANT" -> Sort.by(Sort.Direction.DESC, "currentParticipants");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // LATEST
        };
    }

    /**
     * 검색 필터 존재 여부 확인
     */
    private boolean hasSearchFilters(CreatingMeetingRequest.MeetingSearchRequest request) {
        return request.getKeyword() != null ||
                request.getCategory() != null ||
                request.getLocation() != null ||
                request.getMinFee() != null ||
                request.getMaxFee() != null ||
                request.getDifficultyLevel() != null;
    }

    /**
     * 필터를 적용한 모임 검색
     */
    private Page<Meeting> searchMeetingsWithFilters(CreatingMeetingRequest.MeetingSearchRequest request, Pageable pageable) {
        // 키워드 검색이 있으면 우선 키워드로 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            return meetingRepository.searchByKeyword(request.getKeyword(), request.getStatus(), pageable);
        }

        // 고급 필터 검색
        return meetingRepository.findMeetingsWithAdvancedFilters(
                request.getCategory(),
                request.getSubcategory(),
                request.getLocation(),
                request.getMinFee(),
                request.getMaxFee(),
                request.getDifficultyLevel(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus(),
                pageable
        );
    }

    /**
     * 모임 필드 업데이트
     */
    private void updateMeetingFields(Meeting meeting, CreatingMeetingRequest.UpdateMeetingRequest request) {
        if (request.getTitle() != null) meeting.setTitle(request.getTitle());
        if (request.getDescription() != null) meeting.setDescription(request.getDescription());
        if (request.getCategory() != null) meeting.setCategory(request.getCategory());
        if (request.getSubcategory() != null) meeting.setSubcategory(request.getSubcategory());
        if (request.getLocation() != null) meeting.setLocation(request.getLocation());
        if (request.getAddress() != null) meeting.setAddress(request.getAddress());
        if (request.getLatitude() != null) meeting.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) meeting.setLongitude(request.getLongitude());
        if (request.getStartDate() != null) meeting.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) meeting.setEndDate(request.getEndDate());
        if (request.getMaxParticipants() != null) meeting.setMaxParticipants(request.getMaxParticipants());
        if (request.getFee() != null) meeting.setFee(request.getFee());
        if (request.getDifficultyLevel() != null) meeting.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getAgeRange() != null) meeting.setAgeRange(request.getAgeRange());
        if (request.getOrganizerContact() != null) meeting.setOrganizerContact(request.getOrganizerContact());
        if (request.getPreparationNeeded() != null) meeting.setPreparationNeeded(request.getPreparationNeeded());
        if (request.getMeetingRules() != null) meeting.setMeetingRules(request.getMeetingRules());
    }

    // ===================== DTO 변환 메서드들 =====================

    /**
     * Meeting -> CreatingMeetingRequest.MeetingResponse 변환
     */
    private CreatingMeetingRequest.MeetingResponse convertToMeetingResponse(Meeting meeting, User currentUser) {
        CreatingMeetingRequest.MeetingResponse response = new CreatingMeetingRequest.MeetingResponse();

        // 기본 정보
        response.setId(meeting.getId());
        response.setTitle(meeting.getTitle());
        response.setDescription(meeting.getDescription());
        response.setCategory(meeting.getCategory());
        response.setSubcategory(meeting.getSubcategory());
        response.setLocation(meeting.getLocation());
        response.setAddress(meeting.getAddress());
        response.setLatitude(meeting.getLatitude());
        response.setLongitude(meeting.getLongitude());
        response.setStartDate(meeting.getStartDate());
        response.setEndDate(meeting.getEndDate());
        response.setMaxParticipants(meeting.getMaxParticipants());
        response.setCurrentParticipants(meeting.getCurrentParticipants());
        response.setFee(meeting.getFee());
        response.setDifficultyLevel(meeting.getDifficultyLevel());
        response.setAgeRange(meeting.getAgeRange());
        response.setMeetingType(meeting.getMeetingType());
        response.setSource(meeting.getSource());
        response.setOrganizerContact(meeting.getOrganizerContact());
        response.setPreparationNeeded(meeting.getPreparationNeeded());
        response.setMeetingRules(meeting.getMeetingRules());
        response.setViewCount(meeting.getViewCount());
        response.setDailyViewCount(meeting.getDailyViewCount());
        response.setWishlistCount(meeting.getWishlistCount());
        response.setShareCount(meeting.getShareCount());
        response.setStatus(meeting.getStatus());
        response.setCreatedAt(meeting.getCreatedAt());
        response.setUpdatedAt(meeting.getUpdatedAt());

        // 주최자 정보
        response.setCreatedBy(createUserSummary(meeting.getCreatedBy()));

        // 사용자별 상태 정보
        setUserContextForResponse(response, meeting.getId(), currentUser);

        // 참가자 목록 (승인된 사용자만, 최대 10명)
        List<MeetingParticipant> approvedParticipants = participantRepository
                .findByMeetingIdAndStatus(meeting.getId(), MeetingParticipant.Status.APPROVED)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        response.setParticipants(approvedParticipants.stream()
                .map(this::convertToParticipantSummary)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Meeting -> CreatingMeetingRequest.MeetingSummary 변환 (목록용)
     */
    private CreatingMeetingRequest.MeetingSummary convertToMeetingSummary(Meeting meeting, User currentUser) {
        CreatingMeetingRequest.MeetingSummary summary = new CreatingMeetingRequest.MeetingSummary();

        summary.setId(meeting.getId());
        summary.setTitle(meeting.getTitle());
        summary.setDescription(meeting.getDescription());
        summary.setCategory(meeting.getCategory());
        summary.setSubcategory(meeting.getSubcategory());
        summary.setLocation(meeting.getLocation());
        summary.setStartDate(meeting.getStartDate());
        summary.setEndDate(meeting.getEndDate());
        summary.setMaxParticipants(meeting.getMaxParticipants());
        summary.setCurrentParticipants(meeting.getCurrentParticipants());
        summary.setFee(meeting.getFee());
        summary.setDifficultyLevel(meeting.getDifficultyLevel());
        summary.setAgeRange(meeting.getAgeRange());
        summary.setViewCount(meeting.getViewCount());
        summary.setWishlistCount(meeting.getWishlistCount());
        summary.setStatus(meeting.getStatus());
        summary.setCreatedAt(meeting.getCreatedAt());

        // 주최자 정보
        summary.setCreatedBy(createUserSummary(meeting.getCreatedBy()));

        // 사용자별 상태 정보
        setUserContextForSummary(summary, meeting.getId(), currentUser);

        return summary;
    }

    /**
     * MeetingParticipant -> ParticipantSummary 변환
     */
    private CreatingMeetingRequest.MeetingResponse.ParticipantSummary convertToParticipantSummary(MeetingParticipant participant) {
        CreatingMeetingRequest.MeetingResponse.ParticipantSummary summary = new CreatingMeetingRequest.MeetingResponse.ParticipantSummary();
        User user = participant.getUser();

        summary.setId(user.getId());
        summary.setName(user.getName());
        summary.setProfileImageUrl(user.getProfileImageUrl());
        // summary.setAge(user.getAge()); // User 엔티티에 age 필드 추가 필요 시
        // summary.setLocation(user.getLocation()); // User 엔티티에 location 필드 추가 필요 시

        return summary;
    }

    /**
     * 찜 여부 확인
     */
    private Boolean isWishlisted(Long meetingId, Long userId) {
        return wishlistRepository.existsByUserIdAndMeetingId(userId, meetingId);
    }

    /**
     * 참여 여부 확인
     */
    private Boolean isParticipating(Long meetingId, Long userId) {
        return participantRepository.hasActiveApplication(meetingId, userId);
    }

    /**
     * 참여 상태 조회
     */
    private String getParticipationStatus(Long meetingId, Long userId) {
        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        return participant.map(p -> p.getStatus().name()).orElse(null);
    }
}