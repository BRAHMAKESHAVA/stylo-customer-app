package org.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.backend.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "booking_approval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "service_duration", nullable = false)
    private Integer serviceDuration;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_start_time", nullable = false)
    private LocalTime slotStartTime;

    @Column(name = "slot_end_time", nullable = false)
    private LocalTime slotEndTime;

    @Column(name = "working_end_time", nullable = false)
    private LocalTime workingEndTime;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.approvalStatus == null) {
            this.approvalStatus = BookingStatus.PENDING_PARTNER_CONFIRMATION.name();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
