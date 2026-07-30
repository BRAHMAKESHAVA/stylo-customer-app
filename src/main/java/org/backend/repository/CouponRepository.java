package org.backend.repository;

import org.backend.enums.CouponStatus;
import org.backend.model.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByIdAndDeletedFalse(UUID id);

    Optional<Coupon> findByCouponCodeIgnoreCaseAndDeletedFalse(String couponCode);

    boolean existsByCouponCodeIgnoreCase(String couponCode);

    boolean existsByCouponCodeIgnoreCaseAndIdNot(String couponCode, UUID id);

    @Query("SELECT c FROM Coupon c WHERE c.deleted = false " +
            "AND (:status IS NULL OR c.status = :status)"
            )
    Page<Coupon> search(
            @Param("status") CouponStatus status,
            Pageable pageable
    );

    // COUPONS CREATED BY A SPECIFIC PARTNER (partner-side "my coupons" listing)
    // createdByType = PARTNER is included as a safety check — partnerId is only ever set
    // when a coupon was created by a partner, but this keeps the query explicit rather
    // than relying on that invariant alone. status is optional so the partner UI can filter
    // to just ACTIVE / EXPIRED / PAUSED / DRAFT, or omit it to see everything.
    @Query("SELECT c FROM Coupon c WHERE c.deleted = false " +
            "AND c.partnerId = :partnerId " +
            "AND c.createdByType = org.backend.enums.CreatedByType.PARTNER " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Coupon> findByPartnerId(
            @Param("partnerId") Long partnerId,
            @Param("status") CouponStatus status,
            Pageable pageable
    );

    // Non-paginated version for summary aggregation — needs every row to sum totals,
    // not a page of them.
    @Query("SELECT c FROM Coupon c WHERE c.deleted = false " +
            "AND c.partnerId = :partnerId " +
            "AND c.createdByType = org.backend.enums.CreatedByType.PARTNER")
    List<Coupon> findAllByPartnerId(@Param("partnerId") Long partnerId);

    // FETCH-TIME CANDIDATES (Section A)
    // "Candidate" because this only applies the cheap, customer-independent filters
    // (deleted / status / visible / date-window). Target-type membership, booking-count
    // range, and usage-limit checks still need to run per customer afterwards in
    // CouponEligibilityService before a coupon can be called truly "eligible".
    // Bill amount / city / payment method are intentionally excluded — those are
    // apply-time-only checks (Section B).
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.deleted = false
            AND c.status = org.backend.enums.CouponStatus.ACTIVE
            AND c.visibleToCustomer = true
            AND c.startDate <= :now
            AND c.endDate >= :now
            """)
    List<Coupon> findCandidateCoupons(@Param("now") LocalDateTime now);
}