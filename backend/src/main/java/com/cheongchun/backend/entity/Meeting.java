package com.cheongchun.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "meetings")
public class Meeting {

    // Getters and Setters
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Meeting other = (Meeting) obj;

        // ID가 둘 다 null이 아닌 경우, ID로 비교
        if (this.id != null && other.id != null) {
            return Objects.equals(this.id, other.id);
        }

        // ID가 null인 경우 (새로 생성된 엔티티), 비즈니스 키로 비교
        // 제목 + 시작시간 + 생성자로 비교 (충분히 unique한 조합)
        return Objects.equals(this.title, other.title) &&
                Objects.equals(this.startDate, other.startDate) &&
                Objects.equals(this.createdBy, other.createdBy);
    }

    @Override
    public int hashCode() {
        // ID가 있으면 ID 기반
        if (this.id != null) {
            return Objects.hash(this.id);
        }

        // ID가 null인 경우 비즈니스 키 기반
        return Objects.hash(this.title, this.startDate, this.createdBy);
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

}