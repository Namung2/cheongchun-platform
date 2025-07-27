package com.cheongchun.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_wishlists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "meeting_id"}))
public class UserWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 생성자
    public UserWishlist() {}

    public UserWishlist(User user, Meeting meeting) {
        this.user = user;
        this.meeting = meeting;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // equals and hashCode (중요: 중복 방지)
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserWishlist that = (UserWishlist) obj;

        if (user != null ? !user.getId().equals(that.user.getId()) : that.user != null) return false;
        return meeting != null ? meeting.getId().equals(that.meeting.getId()) : that.meeting == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.getId().hashCode() : 0;
        result = 31 * result + (meeting != null ? meeting.getId().hashCode() : 0);
        return result;
    }
}