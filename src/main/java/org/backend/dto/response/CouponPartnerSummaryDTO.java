package org.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPartnerSummaryDTO {

    private Long partnerId;

    // Number of Coupon rows (deleted = false) created by this partner — i.e. how many
    // coupon records exist, regardless of STATIC/GENERATED or status.
    private long totalCouponRecords;

    // Sum of each row's contribution: STATIC rows contribute 1, GENERATED rows contribute
    // their own generatedCouponCount. Same rule as the single-code summary, just summed.
    private long totalCoupons;

    // Count of rows currently usable: status <> EXPIRED and within startDate/endDate.
    // Same per-row rule as the single-code summary (includes ACTIVE, PAUSED, DRAFT rows).
    private long currentlyValidCoupons;

    // Successful redemptions (status = REDEEMED) across every coupon row created by this partner.
    private long redeemedCount;

    // Redemptions later cancelled (status = CANCELLED) across every coupon row created by this partner.
    private long cancelledCount;

    // totalCoupons - redeemedCount - cancelledCount, floored at 0.
    private long remainingCount;
}