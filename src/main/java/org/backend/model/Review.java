package org.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "salon_id", nullable = false)
    private Long salonId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
}