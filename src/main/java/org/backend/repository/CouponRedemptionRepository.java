package org.backend.repository;

import org.backend.enums.RedemptionStatus;
import org.backend.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    // GLOBAL USAGE LIMIT — total successful redemptions across all customers
    long countByCoupon_IdAndStatus(UUID couponId, RedemptionStatus status);

    // PER-PARTNER USAGE LIMIT — only meaningful when the coupon itself is tied to one fixed
    // partner (createdByType = PARTNER). partnerId is stored directly on CouponRedemption,
    // no join needed.
    long countByCoupon_IdAndPartnerIdAndStatus(UUID couponId, Long partnerId, RedemptionStatus status);

    // PER-USER USAGE LIMIT — CouponRedemption doesn't store customerId directly, so this
    // joins to Booking via bookingId to resolve which customer made the redemption.
    @Query("""
            SELECT COUNT(cr)
            FROM CouponRedemption cr
            JOIN Booking b ON b.bookingId = cr.bookingId
            WHERE cr.coupon.id = :couponId
            AND b.customerId = :customerId
            AND cr.status = org.backend.enums.RedemptionStatus.REDEEMED
            """)
    long countByCouponIdAndCustomerId(
            @Param("couponId") UUID couponId,
            @Param("customerId") Long customerId
    );

    // PARTNER SUMMARY — aggregate redemption count across every coupon row created by a
    // partner, in one query instead of one call per row.
    long countByCoupon_IdInAndStatus(List<UUID> couponIds, RedemptionStatus status);

    // DAILY USAGE LIMIT — successful redemptions within a given calendar-day window
    @Query("""
            SELECT COUNT(cr)
            FROM CouponRedemption cr
            WHERE cr.coupon.id = :couponId
            AND cr.status = org.backend.enums.RedemptionStatus.REDEEMED
            AND cr.createdAt >= :startOfDay
            AND cr.createdAt < :endOfDay
            """)
    long countByCouponIdAndCreatedAtBetween(
            @Param("couponId") UUID couponId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}