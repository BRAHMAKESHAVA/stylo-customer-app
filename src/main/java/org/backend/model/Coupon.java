package org.backend.model;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.backend.enums.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Basic Details
    @Column(name = "coupon_code", nullable = false, unique = true, length = 50)
    private String couponCode;

    @Column(name = "coupon_name", nullable = false, length = 200)
    private String couponName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Ownership
    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", length = 20, nullable = false)
    private CreatedByType createdByType;

    @Column(name = "partner_id")
    private Long partnerId;    //Mandatory if created_by_type = PARTNER

    // Discount Configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 30, nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "maximum_discount_amount", precision = 12, scale = 2)
    private BigDecimal maximumDiscountAmount;

    // Bill Amount Eligibility
    @Column(name = "minimum_bill_amount", precision = 12, scale = 2)
    private BigDecimal minimumBillAmount;

    @Column(name = "maximum_bill_amount", precision = 12, scale = 2)
    private BigDecimal maximumBillAmount;

    // Booking Eligibility
    @Column(name = "minimum_booking_count")
    private Integer minimumBookingCount;

    @Column(name = "maximum_booking_count")
    private Integer maximumBookingCount;

    // Coupon Target
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30, nullable = false)
    private TargetType targetType;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "eligible_customer_ids", columnDefinition = "uuid[]")
    private List<Long> eligibleCustomerIds = new ArrayList<>(); // Mandatory if target_type = any except ALL

    @Builder.Default
    @Column(name = "existing_partner_customer_only")
    private Boolean existingPartnerCustomerOnly = Boolean.FALSE;

    // Restrictions
    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "eligible_cities", columnDefinition = "varchar[]")
    private List<String> eligibleCities = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "eligible_payment_methods", columnDefinition = "varchar[]")
    private List<String> eligiblePaymentMethods = new ArrayList<>();

    @jakarta.persistence.Transient
    public List<PaymentMethod> getEligiblePaymentMethodValues() {
        return this.eligiblePaymentMethods == null
                ? new ArrayList<>()
                : this.eligiblePaymentMethods.stream().map(PaymentMethod::valueOf).toList();
    }

    public void setEligiblePaymentMethodValues(List<PaymentMethod> paymentMethods) {
        this.eligiblePaymentMethods = paymentMethods == null
                ? new ArrayList<>()
                : paymentMethods.stream().map(Enum::name).toList();
    }

    // Validity
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // Usage Limits
    @Column(name = "global_usage_limit")
    private Integer globalUsageLimit;

    @Column(name = "per_user_usage_limit")
    private Integer perUserUsageLimit;

    @Column(name = "per_partner_usage_limit")
    private Integer perPartnerUsageLimit;

    @Column(name = "daily_usage_limit")
    private Integer dailyUsageLimit;

    // Generated Coupons
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", length = 20)
    private CouponGenerationType generationType = CouponGenerationType.STATIC;

    @Column(name = "generated_coupon_count")
    private Integer generatedCouponCount;  // Mandatory if generation_type = DYNAMIC

    // Visibility
    @Builder.Default
    @Column(name = "visible_to_customer")
    private Boolean visibleToCustomer = Boolean.TRUE;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CouponStatus status;

    // Soft Delete
    @Builder.Default
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Audit
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}