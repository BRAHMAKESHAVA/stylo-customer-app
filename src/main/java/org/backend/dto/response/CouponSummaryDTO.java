package org.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.backend.enums.CouponGenerationType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSummaryDTO {

    private String couponCode;

    private CouponGenerationType generationType;

    // STATIC -> always 1. GENERATED -> the coupon row's own generatedCouponCount field
    // (the batch size configured at creation time). No cross-row linking exists today, so
    // this reflects the configured total, not an actual count of related rows.
    private long totalCoupons;

    // 1 if this row is currently usable (deleted = false, status <> EXPIRED, and within
    // startDate/endDate), otherwise 0.
    private long currentlyValidCoupons;

    // Successful redemptions (status = REDEEMED) against this coupon's id.
    private long redeemedCount;

    // Redemptions that were later cancelled (status = CANCELLED) against this coupon's id.
    private long cancelledCount;

    // totalCoupons - redeemedCount - cancelledCount, floored at 0. A cancelled redemption
    // still consumed a code (it was used, then later cancelled), so it counts against
    // remaining just like a successful redemption does.
    private long remainingCount;
}