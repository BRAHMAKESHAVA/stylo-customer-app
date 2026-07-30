package org.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.backend.enums.CouponGenerationType;
import org.backend.enums.CouponStatus;
import org.backend.enums.CreatedByType;
import org.backend.enums.DiscountType;
import org.backend.enums.PaymentMethod;
import org.backend.enums.TargetType;
import org.backend.validation.annotation.ValidCoupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCoupon
public class CouponRequestDTO {

    // Basic Details
    @NotBlank(message = "couponCode is mandatory")
    @Size(max = 50, message = "couponCode must not exceed 50 characters")
    private String couponCode;

    @NotBlank(message = "couponName is mandatory")
    @Size(max = 200, message = "couponName must not exceed 200 characters")
    private String couponName;

    private String description;

    // Ownership
    @NotNull(message = "createdByType is mandatory")
    private CreatedByType createdByType;

    private Long partnerId;  // Mandatory only when createdByType = PARTNER

    // Discount Configuration
    @NotNull(message = "discountType is mandatory")
    private DiscountType discountType;

    @NotNull(message = "discountValue is mandatory")
    @DecimalMin(value = "0.01", message = "discountValue must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = false, message = "maximumDiscountAmount must be greater than 0")
    private BigDecimal maximumDiscountAmount;

    // Bill Amount Eligibility
    @DecimalMin(value = "0.0", message = "minimumBillAmount cannot be negative")
    private BigDecimal minimumBillAmount;

    @DecimalMin(value = "0.0", message = "maximumBillAmount cannot be negative")
    private BigDecimal maximumBillAmount;

    // Booking Eligibility
    @Min(value = 0, message = "minimumBookingCount cannot be negative")
    private Integer minimumBookingCount;

    @Min(value = 0, message = "maximumBookingCount cannot be negative")
    private Integer maximumBookingCount;

    // Coupon Target
    @NotNull(message = "targetType is mandatory")
    private TargetType targetType;

    @Builder.Default
    private List<Long> eligibleCustomerIds = List.of(); // Mandatory when targetType = SPECIFIC_CUSTOMERS

    private Boolean existingPartnerCustomerOnly;

    // Restrictions
    @Builder.Default
    private List<String> eligibleCities = List.of();

    @Builder.Default
    private List<PaymentMethod> eligiblePaymentMethods = List.of();

    // Validity
    @NotNull(message = "startDate is mandatory")
    private LocalDateTime startDate;

    @NotNull(message = "endDate is mandatory")
    private LocalDateTime endDate;

    // Usage Limits
    @Min(value = 1, message = "globalUsageLimit must be at least 1")
    private Integer globalUsageLimit;

    @Min(value = 1, message = "perUserUsageLimit must be at least 1")
    private Integer perUserUsageLimit;

    @Min(value = 1, message = "perPartnerUsageLimit must be at least 1")
    private Integer perPartnerUsageLimit;

    @Min(value = 1, message = "dailyUsageLimit must be at least 1")
    private Integer dailyUsageLimit;

    // Generated Coupons
    @Builder.Default
    private CouponGenerationType generationType = CouponGenerationType.STATIC;

    private Integer generatedCouponCount; // Mandatory when generationType = DYNAMIC

    // Visibility
    @Builder.Default
    private Boolean visibleToCustomer = Boolean.TRUE;

    // Status
    @NotNull(message = "status is mandatory")
    private CouponStatus status;

    private UUID createdBy;

    private UUID updatedBy;
}
