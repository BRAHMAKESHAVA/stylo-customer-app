package org.backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.backend.dto.PackageResponseDTO;
import org.backend.dto.ServiceInfoDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Booking Response",
        description = "DTO representing the details of a booking including pricing, status, timings, and refund information."
)
public class BookingResponseDTO {

    @Schema(description = "Unique identifier of the booking", example = "1001")
    private UUID bookingId;

    @Schema(description = "Gross amount before deductions", example = "2000.00")
    private BigDecimal grossAmount;

    @Schema(description = "Platform fee charged", example = "100.00")
    private BigDecimal platformFee;

    @Schema(description = "Commission amount charged", example = "150.00")
    private BigDecimal commissionAmount;

    @Schema(description = "Tax amount applied", example = "180.00")
    private BigDecimal taxAmount;

    @Schema(description = "Discount amount applied", example = "200.00")
    private BigDecimal discountAmount;

    @Schema(description = "Final payable amount after deductions", example = "1930.00")
    private BigDecimal finalAmount;

    @Schema(description = "Current booking status", example = "CONFIRMED")
    private String status;

    @Schema(description = "Payment provider used", example = "RAZORPAY")
    private String paymentProvider;

    @Schema(description = "Payment status", example = "SUCCESS")
    private String paymentStatus;

    @Schema(description = "Booking start time", example = "2026-06-04T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "Booking end time", example = "2026-06-04T11:30:00")
    private LocalDateTime endTime;

    @Schema(description = "Booking creation timestamp", example = "2026-06-01T09:15:00")
    private LocalDateTime createdDate;

    @Schema(description = "Name of the salon", example = "Stylo Salon")
    private String salonName;

    @Schema(description = "Total duration of the booking in minutes", example = "90")
    private Long totalDuration;

    @Schema(description = "Refund amount if applicable", example = "500.00")
    private String refundAmount;

    @Schema(description = "Refund tier applied", example = "PARTIAL")
    private String refundTier;

    @Schema(description = "Reason for rejection if booking was cancelled", example = "Stylist unavailable")
    private String rejectionReason;

    private PackageResponseDTO packageDetails;

    private List<ServiceInfoDTO> addOnServices;

// Constructor for your current usage
    public BookingResponseDTO(
            UUID bookingId,
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