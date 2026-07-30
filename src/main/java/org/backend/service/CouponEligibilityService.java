package org.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.enums.CreatedByType;
import org.backend.enums.RedemptionStatus;
import org.backend.model.Coupon;
import org.backend.repository.BookingRepository;
import org.backend.repository.CouponRedemptionRepository;
import org.backend.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resolves which coupons a customer is eligible to SEE (GET coupons by userId),
 * irrespective of apply-time-only conditions like bill amount, city, or payment method.
 *
 * Section A checks applied here:
 *  1. deleted / status / visibleToCustomer / validity window   -> CouponRepository.findCandidateCoupons
 *  2. targetType (ALL / SELECTED_USERS / PARTNER_CUSTOMERS / FIRST_BOOKING / RETURNING_CUSTOMERS —
 *     RETURNING_CUSTOMERS means "lapsed": has booking history, but nothing in the last
 *     RETURNING_INACTIVITY_MONTHS months. Just "completedBookingCount > 0" is NOT enough.)
 *  3. minimumBookingCount / maximumBookingCount
 *  4. globalUsageLimit / perUserUsageLimit / dailyUsageLimit / perPartnerUsageLimit (fixed-partner coupons only)
 *
 * Section B checks (minimumBillAmount, maximumBillAmount, eligibleCities, eligiblePaymentMethods,
 * and perPartnerUsageLimit for non-fixed-partner coupons) are intentionally NOT applied here —
 * they belong to the apply-time validation flow, since they need a specific booking/cart context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponEligibilityService {

    private static final String 

            COMPLETED_STATUS = "COMPLETED";

    // A "returning" customer is one who has booking history but has been inactive for at
    // least this many months. Adjust here (or externalize to config) if the business rule changes.
    private static final int RETURNING_INACTIVITY_MONTHS = 6;

    private final CouponRepository couponRepository;
    private final BookingRepository bookingRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    @Transactional(readOnly = true)
    public List<Coupon> getEligibleCouponsForUser(Long customerId) {

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> candidates = couponRepository.findCandidateCoupons(now);

        long completedBookingCount = bookingRepository.countByCustomerIdAndStatus(customerId, COMPLETED_STATUS);
        LocalDateTime lastCompletedBookingDate = completedBookingCount > 0
                ? bookingRepository.findLastCompletedBookingDate(customerId).orElse(null)
                : null;

        List<Coupon> eligible = new ArrayList<>();
        for (Coupon coupon : candidates) {
            if (!passesTargetType(coupon, customerId, completedBookingCount, lastCompletedBookingDate, now)) {
                continue;
            }
            if (!passesBookingCountRange(coupon, completedBookingCount)) {
                continue;
            }
            if (!passesUsageLimits(coupon, customerId, now)) {
                continue;
            }
            eligible.add(coupon);
        }

        log.info("Resolved {} eligible coupon(s) for customerId={}", eligible.size(), customerId);
        return eligible;
    }

    // Condition set: targetType
    private boolean passesTargetType(
            Coupon coupon,
            Long customerId,
            long completedBookingCount,
            LocalDateTime lastCompletedBookingDate,
            LocalDateTime now
    ) {
        switch (coupon.getTargetType()) {
            case ALL:
                return true;

            case SELECTED_USERS:
                return coupon.getEligibleCustomerIds() != null
                        && coupon.getEligibleCustomerIds().contains(customerId);

            case PARTNER_CUSTOMERS:
                return coupon.getPartnerId() != null
                        && bookingRepository.existsCompletedBookingByCustomerIdAndPartnerId(
                        customerId, coupon.getPartnerId());

            case FIRST_BOOKING:
                return completedBookingCount == 0;

            case RETURNING_CUSTOMERS:
                // "Returning" here means LAPSED: has booking history, but their last completed
                // booking was more than RETURNING_INACTIVITY_MONTHS months ago. A customer who
                // booked yesterday is currently active, not "returning" — win-back coupons
                // shouldn't be shown to them.
                if (completedBookingCount == 0 || lastCompletedBookingDate == null) {
                    return false;
                }
                LocalDateTime inactivityCutoff = now.minusMonths(RETURNING_INACTIVITY_MONTHS);
                return lastCompletedBookingDate.isBefore(inactivityCutoff);

            default:
                return false;
        }
    }

    // Condition set: minimumBookingCount / maximumBookingCount
    private boolean passesBookingCountRange(Coupon coupon, long completedBookingCount) {
        if (coupon.getMinimumBookingCount() != null && completedBookingCount < coupon.getMinimumBookingCount()) {
            return false;
        }
        if (coupon.getMaximumBookingCount() != null && completedBookingCount > coupon.getMaximumBookingCount()) {
            return false;
        }
        return true;
    }

    // Condition set: globalUsageLimit / perUserUsageLimit / dailyUsageLimit / perPartnerUsageLimit
    private boolean passesUsageLimits(Coupon coupon, Long customerId, LocalDateTime now) {
        UUID couponId = coupon.getId();

        if (coupon.getGlobalUsageLimit() != null) {
            long globalCount = couponRedemptionRepository.countByCoupon_IdAndStatus(couponId, RedemptionStatus.REDEEMED);
            if (globalCount >= coupon.getGlobalUsageLimit()) {
                return false;
            }
        }

        if (coupon.getPerUserUsageLimit() != null) {
            long userCount = couponRedemptionRepository.countByCouponIdAndCustomerId(couponId, customerId);
            if (userCount >= coupon.getPerUserUsageLimit()) {
                return false;
            }
        }

        if (coupon.getDailyUsageLimit() != null) {
            LocalDate today = now.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            long dailyCount = couponRedemptionRepository.countByCouponIdAndCreatedAtBetween(
                    couponId, startOfDay, endOfDay);
            if (dailyCount >= coupon.getDailyUsageLimit()) {
                return false;
            }
        }

        // perPartnerUsageLimit can only be resolved here for coupons tied to one fixed partner
        // (createdByType = PARTNER). For admin-created coupons usable across many partners,
        // this is deferred to apply-time since it depends on which partner's booking is being paid for.
        if (coupon.getPerPartnerUsageLimit() != null
                && coupon.getCreatedByType() == CreatedByType.PARTNER
                && coupon.getPartnerId() != null) {
            long partnerCount = couponRedemptionRepository.countByCoupon_IdAndPartnerIdAndStatus(
                    couponId, coupon.getPartnerId(), RedemptionStatus.REDEEMED);
            if (partnerCount >= coupon.getPerPartnerUsageLimit()) {
                return false;
            }
        }

        return true;
    }
}