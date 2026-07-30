package org.backend.dto.response;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only representation of a coupon returned by the API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDTO {

    private UUID id;

    private String couponCode;
    private String couponName;
    private String description;

    private CreatedByType createdByType;
    private Long partnerId;

    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maximumDiscountAmount;

    private BigDecimal minimumBillAmount;
    private BigDecimal maximumBillAmount;

    private Integer minimumBookingCount;
    private Integer maximumBookingCount;

    private TargetType targetType;
    private List<Long> eligibleCustomerIds;
    private Boolean existingPartnerCustomerOnly;

    private List<String> eligibleCities;
    private List<PaymentMethod> eligiblePaymentMethods;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer globalUsageLimit;
    private Integer perUserUsageLimit;
    private Integer perPartnerUsageLimit;
    private Integer dailyUsageLimit;

    private CouponGenerationType generationType;
    private Integer generatedCouponCount;

    private Boolean visibleToCustomer;

    private CouponStatus status;

    private Boolean deleted;
    private LocalDateTime deletedAt;

    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
}
