package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.WishlistDto;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.entity.UserWishlist;
import com.cheongchun.backend.repository.MeetingRepository;
import com.cheongchun.backend.repository.UserWishlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WishlistService {

    private final UserWishlistRepository wishlistRepository;
    private final MeetingRepository meetingRepository;

    public WishlistService(UserWishlistRepository wishlistRepository,
                           MeetingRepository meetingRepository) {
        this.wishlistRepository = wishlistRepository;
        this.meetingRepository = meetingRepository;
    }

    /**
     * 찜 추가
     */
    public WishlistDto.WishlistResponse addToWishlist(Long meetingId, User currentUser) {
        // 모임 존재 여부 확인
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("모임을 찾을 수 없습니다"));

        // 이미 찜한 모임인지 확인
        if (wishlistRepository.existsByUserAndMeeting(currentUser, meeting)) {
            throw new RuntimeException("이미 찜한 모임입니다");
        }

        // 자신이 주최한 모임은 찜할 수 없음
        if (meeting.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("자신이 주최한 모임은 찜할 수 없습니다");
        }

        // 찜 추가
        UserWishlist wishlist = new UserWishlist(currentUser, meeting);
        UserWishlist savedWishlist = wishlistRepository.save(wishlist);

        // 모임의 찜 개수 업데이트
        updateMeetingWishlistCount(meetingId);

        return convertToWishlistResponse(savedWishlist);
    }

    /**
     * 찜 삭제
     */
    public void removeFromWishlist(Long meetingId, User currentUser) {
        UserWishlist wishlist = wishlistRepository.findByUserIdAndMeetingId(currentUser.getId(), meetingId)
                .orElseThrow(() -> new RuntimeException("찜한 모임을 찾을 수 없습니다"));

        wishlistRepository.delete(wishlist);

        // 모임의 찜 개수 업데이트
        updateMeetingWishlistCount(meetingId);
    }

    /**
     * 찜 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<WishlistDto.WishlistSummary> getMyWishlist(User currentUser,
                                                           WishlistDto.WishlistSearchRequest searchRequest) {
        Pageable pageable = createPageable(searchRequest);
        Page<UserWishlist> wishlists;

        // 필터링 조건에 따른 조회
        if (searchRequest.getCategory() != null) {
            wishlists = wishlistRepository.findByUserIdAndMeetingCategory(
                    currentUser.getId(), searchRequest.getCategory(), pageable);
        } else if (searchRequest.getLocation() != null && !searchRequest.getLocation().trim().isEmpty()) {
            wishlists = wishlistRepository.findByUserIdAndMeetingLocation(
                    currentUser.getId(), searchRequest.getLocation(), pageable);
        } else if (searchRequest.getStatus() != null) {
            wishlists = wishlistRepository.findByUserIdAndMeetingStatus(
                    currentUser.getId(), searchRequest.getStatus(), pageable);
        } else {
            wishlists = wishlistRepository.findByUserIdWithMeeting(currentUser.getId(), pageable);
        }

        return wishlists.map(this::convertToWishlistSummary);
    }

    /**
     * 찜한 모임 중 곧 시작되는 모임
     */
    @Transactional(readOnly = true)
    public List<WishlistDto.UpcomingWishlistSummary> getUpcomingWishlistedMeetings(User currentUser) {
        List<UserWishlist> upcomingWishlists = wishlistRepository.findUpcomingWishlistedMeetings(currentUser.getId());

        return upcomingWishlists.stream()
                .map(this::convertToUpcomingWishlistSummary)
                .collect(Collectors.toList());
    }

    /**
     * 찜 통계 조회
     */
    @Transactional(readOnly = true)
    public WishlistDto.WishlistStats getWishlistStats(User currentUser) {
        List<Object[]> categoryStats = wishlistRepository.getWishlistStatsByCategory(currentUser.getId());
        long totalCount = wishlistRepository.countByUserId(currentUser.getId());

        WishlistDto.WishlistStats stats = new WishlistDto.WishlistStats();
        stats.setUserId(currentUser.getId());
        stats.setTotalWishlistCount((int) totalCount);

        // 카테고리별 통계 설정
        for (Object[] stat : categoryStats) {
            Meeting.Category category = (Meeting.Category) stat[0];
            Long count = (Long) stat[1];

            switch (category) {
                case HOBBY -> stats.setHobbyCount(count.intValue());
                case EXERCISE -> stats.setExerciseCount(count.intValue());
                case CULTURE -> stats.setCultureCount(count.intValue());
                case EDUCATION -> stats.setEducationCount(count.intValue());
                case TALK -> stats.setTalkCount(count.intValue());
                case VOLUNTEER -> stats.setVolunteerCount(count.intValue());
            }
        }

        return stats;
    }

    /**
     * 찜 여부 확인
     */
    @Transactional(readOnly = true)
    public Boolean isWishlisted(Long meetingId, User currentUser) {
        return wishlistRepository.existsByUserIdAndMeetingId(currentUser.getId(), meetingId);
    }

    /**
     * 찜했지만 아직 신청하지 않은 모임
     */
    @Transactional(readOnly = true)
    public List<WishlistDto.WishlistSummary> getWishlistedButNotApplied(User currentUser) {
        List<UserWishlist> wishlists = wishlistRepository.findWishlistedButNotApplied(currentUser.getId());

        return wishlists.stream()
                .map(this::convertToWishlistSummary)
                .collect(Collectors.toList());
    }

    /**
     * 무료 찜 모임 조회
     */
    @Transactional(readOnly = true)
    public List<WishlistDto.WishlistSummary> getFreeWishlistedMeetings(User currentUser) {
        List<UserWishlist> freeWishlists = wishlistRepository.findFreeWishlistedMeetings(currentUser.getId());

        return freeWishlists.stream()
                .map(this::convertToWishlistSummary)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간에 찜한 모임들
     */
    @Transactional(readOnly = true)
    public List<WishlistDto.WishlistSummary> getWishlistBetweenDates(User currentUser,
                                                                     LocalDateTime startDate,
                                                                     LocalDateTime endDate) {
        List<UserWishlist> wishlists = wishlistRepository.findWishlistBetweenDates(currentUser.getId(), startDate, endDate);

        return wishlists.stream()
                .map(this::convertToWishlistSummary)
                .collect(Collectors.toList());
    }

    /**
     * 찜 목록에서 모임 비교용 ID 목록 조회 (AI 추천에 활용)
     */
    @Transactional(readOnly = true)
    public List<Long> getWishlistedMeetingIds(User currentUser) {
        return wishlistRepository.findMeetingIdsByUserId(currentUser.getId());
    }

    /**
     * 인기 모임 (찜이 많은 순)
     */
    @Transactional(readOnly = true)
    public Page<WishlistDto.PopularMeetingSummary> getPopularMeetingsByWishlist(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = wishlistRepository.findPopularMeetingsByWishlist(pageable);

        return results.map(result -> {
            Meeting meeting = (Meeting) result[0];
            Long wishlistCount = (Long) result[1];

            WishlistDto.PopularMeetingSummary summary = new WishlistDto.PopularMeetingSummary();
            summary.setMeetingId(meeting.getId());
            summary.setMeetingTitle(meeting.getTitle());
            summary.setMeetingCategory(meeting.getCategory());
            summary.setMeetingLocation(meeting.getLocation());
            summary.setMeetingStartDate(meeting.getStartDate());
            summary.setMeetingFee(meeting.getFee());
            summary.setWishlistCount(wishlistCount.intValue());
            summary.setCurrentParticipants(meeting.getCurrentParticipants());
            summary.setMaxParticipants(meeting.getMaxParticipants());

            return summary;
        });
    }

    /**
     * 배치 찜 추가/삭제
     */
    public WishlistDto.BatchWishlistResponse batchWishlistAction(WishlistDto.BatchWishlistRequest request,
                                                                 User currentUser) {
        WishlistDto.BatchWishlistResponse response = new WishlistDto.BatchWishlistResponse();
        response.setSuccessCount(0);
        response.setFailureCount(0);
        response.setErrors(new java.util.ArrayList<>());

        for (Long meetingId : request.getMeetingIds()) {
            try {
                if (request.getAction() == WishlistDto.BatchWishlistRequest.Action.ADD) {
                    addToWishlist(meetingId, currentUser);
                } else {
                    removeFromWishlist(meetingId, currentUser);
                }
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (Exception e) {
                response.setFailureCount(response.getFailureCount() + 1);
                response.getErrors().add("모임 ID " + meetingId + ": " + e.getMessage());
            }
        }

        return response;
    }

    // ==================== 헬퍼 메서드들 ====================

    /**
     * 모임의 찜 개수 업데이트
     */
    private void updateMeetingWishlistCount(Long meetingId) {
        try {
            meetingRepository.updateWishlistCount(meetingId);
        } catch (Exception e) {
            // 찜 개수 업데이트 실패는 전체 로직에 영향주지 않음
            System.err.println("찜 개수 업데이트 실패: " + e.getMessage());
        }
    }

    /**
     * 페이지 설정 생성
     */
    private Pageable createPageable(WishlistDto.WishlistSearchRequest searchRequest) {
        Sort sort = createSort(searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    /**
     * 정렬 설정 생성
     */
    private Sort createSort(String sortBy) {
        return switch (sortBy.toUpperCase()) {
            case "OLDEST" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "MEETING_START_DATE" -> Sort.by(Sort.Direction.ASC, "meeting.startDate");
            case "MEETING_POPULARITY" -> Sort.by(Sort.Direction.DESC, "meeting.viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // LATEST
        };
    }

    // ==================== DTO 변환 메서드들 ====================

    private WishlistDto.WishlistResponse convertToWishlistResponse(UserWishlist wishlist) {
        WishlistDto.WishlistResponse response = new WishlistDto.WishlistResponse();
        response.setId(wishlist.getId());
        response.setUserId(wishlist.getUser().getId());
        response.setMeetingId(wishlist.getMeeting().getId());
        response.setCreatedAt(wishlist.getCreatedAt());

        // 모임 정보
        Meeting meeting = wishlist.getMeeting();
        response.setMeetingTitle(meeting.getTitle());
        response.setMeetingCategory(meeting.getCategory());
        response.setMeetingLocation(meeting.getLocation());
        response.setMeetingStartDate(meeting.getStartDate());
        response.setMeetingFee(meeting.getFee());
        response.setMeetingStatus(meeting.getStatus());

        return response;
    }

    private WishlistDto.WishlistSummary convertToWishlistSummary(UserWishlist wishlist) {
        WishlistDto.WishlistSummary summary = new WishlistDto.WishlistSummary();
        summary.setId(wishlist.getId());
        summary.setAddedAt(wishlist.getCreatedAt());

        // 모임 정보
        Meeting meeting = wishlist.getMeeting();
        summary.setMeetingId(meeting.getId());
        summary.setMeetingTitle(meeting.getTitle());
        summary.setMeetingDescription(meeting.getDescription());
        summary.setMeetingCategory(meeting.getCategory());
        summary.setMeetingSubcategory(meeting.getSubcategory());
        summary.setMeetingLocation(meeting.getLocation());
        summary.setMeetingStartDate(meeting.getStartDate());
        summary.setMeetingEndDate(meeting.getEndDate());
        summary.setMeetingFee(meeting.getFee());
        summary.setMeetingCurrentParticipants(meeting.getCurrentParticipants());
        summary.setMeetingMaxParticipants(meeting.getMaxParticipants());
        summary.setMeetingStatus(meeting.getStatus());
        summary.setMeetingViewCount(meeting.getViewCount());
        summary.setMeetingWishlistCount(meeting.getWishlistCount());

        return summary;
    }

    private WishlistDto.UpcomingWishlistSummary convertToUpcomingWishlistSummary(UserWishlist wishlist) {
        WishlistDto.UpcomingWishlistSummary summary = new WishlistDto.UpcomingWishlistSummary();

        Meeting meeting = wishlist.getMeeting();
        summary.setMeetingId(meeting.getId());
        summary.setMeetingTitle(meeting.getTitle());
        summary.setMeetingLocation(meeting.getLocation());
        summary.setMeetingStartDate(meeting.getStartDate());
        summary.setMeetingEndDate(meeting.getEndDate());
        summary.setAddedAt(wishlist.getCreatedAt());

        // D-Day 계산
        long daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                meeting.getStartDate().toLocalDate()
        );
        summary.setDaysUntilStart((int) daysUntilStart);

        return summary;
    }
}