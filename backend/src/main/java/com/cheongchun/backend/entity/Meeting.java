package com.cheongchun.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @Column(length = 50)
    private String subcategory;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @PositiveOrZero
    @Column(name = "current_participants")
    private Integer currentParticipants = 0;

    @PositiveOrZero
    private Integer fee = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel = DifficultyLevel.BEGINNER;

    @Column(name = "age_range", length = 50)
    private String ageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", length = 20)
    private MeetingType meetingType = MeetingType.USER_CREATED;

    @Column(length = 100)
    private String source;

    @Column(name = "organizer_contact", length = 100)
    private String organizerContact;

    @Column(name = "preparation_needed", columnDefinition = "TEXT")
    private String preparationNeeded;

    @Column(name = "meeting_rules", columnDefinition = "TEXT")
    private String meetingRules;

    // 통계 정보
    @PositiveOrZero
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @PositiveOrZero
    @Column(name = "daily_view_count")
    private Integer dailyViewCount = 0;

    @PositiveOrZero
    @Column(name = "wishlist_count")
    private Integer wishlistCount = 0;

    @PositiveOrZero
    @Column(name = "share_count")
    private Integer shareCount = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.RECRUITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 연관 관계
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MeetingParticipant> participants;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserWishlist> wishlists;

    // Enum 정의
    public enum Category {
        HOBBY, EXERCISE, CULTURE, EDUCATION, TALK, VOLUNTEER
    }

    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum MeetingType {
        USER_CREATED, CRAWLED
    }

    public enum Status {
        RECRUITING, FULL, CLOSED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public boolean canJoin() {
        return status == Status.RECRUITING &&
                (maxParticipants == null || currentParticipants < maxParticipants);
    }

    public boolean isFull() {
        return maxParticipants != null && currentParticipants >= maxParticipants;
    }

    public void incrementParticipants() {
        this.currentParticipants = (this.currentParticipants == null ? 0 : this.currentParticipants) + 1;
        if (isFull()) {
            this.status = Status.FULL;
        }
    }

    public void decrementParticipants() {
        if (this.currentParticipants != null && this.currentParticipants > 0) {
            this.currentParticipants--;
            if (this.status == Status.FULL && canJoin()) {
                this.status = Status.RECRUITING;
            }
        }
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
        this.dailyViewCount = (this.dailyViewCount == null ? 0 : this.dailyViewCount) + 1;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Integer getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(Integer currentParticipants) { this.currentParticipants = currentParticipants; }

    public Integer getFee() { return fee; }
    public void setFee(Integer fee) { this.fee = fee; }

    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(DifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getAgeRange() { return ageRange; }
    public void setAgeRange(String ageRange) { this.ageRange = ageRange; }

    public MeetingType getMeetingType() { return meetingType; }
    public void setMeetingType(MeetingType meetingType) { this.meetingType = meetingType; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getOrganizerContact() { return organizerContact; }
    public void setOrganizerContact(String organizerContact) { this.organizerContact = organizerContact; }

    public String getPreparationNeeded() { return preparationNeeded; }
    public void setPreparationNeeded(String preparationNeeded) { this.preparationNeeded = preparationNeeded; }

    public String getMeetingRules() { return meetingRules; }
    public void setMeetingRules(String meetingRules) { this.meetingRules = meetingRules; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getDailyViewCount() { return dailyViewCount; }
    public void setDailyViewCount(Integer dailyViewCount) { this.dailyViewCount = dailyViewCount; }

    public Integer getWishlistCount() { return wishlistCount; }
    public void setWishlistCount(Integer wishlistCount) { this.wishlistCount = wishlistCount; }

    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<MeetingParticipant> getParticipants() { return participants; }
    public void setParticipants(Set<MeetingParticipant> participants) { this.participants = participants; }

    public Set<UserWishlist> getWishlists() { return wishlists; }
    public void setWishlists(Set<UserWishlist> wishlists) { this.wishlists = wishlists; }
}