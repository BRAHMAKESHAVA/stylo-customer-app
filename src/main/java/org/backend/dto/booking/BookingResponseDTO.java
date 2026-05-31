package org.backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponseDTO {

    private Long bookingId;

    private BigDecimal grossAmount;
    private BigDecimal platformFee;
    private BigDecimal commissionAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private String status;
    private String paymentProvider;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdDate;

    private String salonName;

    private Long totalDuration;

    private String refundAmount;
    private String refundTier;
    private String rejectionReason;

    // Constructor for your current usage
    public BookingResponseDTO(
            Long bookingId,
            BigDecimal grossAmount,
            BigDecimal platformFee,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long totalDuration
    ) {
        this.bookingId = bookingId;
        this.grossAmount = grossAmount;
        this.platformFee = platformFee;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalDuration = totalDuration;
    }

    //pricing summary
    public BookingResponseDTO(
            BigDecimal grossAmount,
            BigDecimal platformFee,
            BigDecimal commissionAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal finalAmount
    ) {
        this.grossAmount = grossAmount;
        this.platformFee = platformFee;
        this.commissionAmount = commissionAmount;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.finalAmount = finalAmount;
    }
}