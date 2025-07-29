package com.cheongchun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"}))
public class MeetingParticipant {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "application_message", columnDefinition = "TEXT")
    private String applicationMessage;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 추가 정보
    @Column(name = "processed_by")
    private Long processedBy; // 승인/거절 처리한 관리자 ID

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Enum 정의
    public enum Status {
        PENDING,    // 신청 대기
        APPROVED,   // 승인됨
        REJECTED,   // 거절됨
        CANCELLED   // 취소됨 (사용자가 직접 취소)
    }

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }

    // 비즈니스 메서드
    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean isApproved() {
        return status == Status.APPROVED;
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    public boolean canCancel() {
        return status == Status.PENDING || status == Status.APPROVED;
    }

    public void approve(Long processedBy) {
        this.status = Status.APPROVED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(Long processedBy, String reason) {
        this.status = Status.REJECTED;
        this.processedBy = processedBy;
        this.rejectionReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.processedAt = LocalDateTime.now();
    }

}