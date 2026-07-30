package org.backend.repository;

import org.backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingIdAndSalonId(
            UUID bookingId,
            Long salonId
    );

    List<Booking> findBySalonIdAndStatus(
            Long salonId,
            String status
    );

    List<Booking> findByStatus(String status);

    List<Booking> findByStatusOrderByCreatedDateDesc(String status);


    @Query("SELECT b FROM Booking b WHERE b.customerId = ?1 AND b.status <> 'PENDING' ORDER BY b.createdDate DESC")
    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findBySalonId(Long salonId);

    @Query("""
                SELECT COUNT(b) > 0 FROM Booking b
                WHERE b.salonId = :salonId
                AND b.status IN :statuses
                AND b.startTime < :end
                AND b.endTime > :start
            """)
    boolean existsOverlappingBooking(
            @Param("salonId") Long salonId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<String> statuses
    );

    @Query("""
                SELECT b
                FROM Booking b
                WHERE b.salonId = :salonId
                AND b.startTime >= :startOfDay
                AND b.startTime < :endOfDay
                AND (
                    b.status IN :statuses
                    OR b.status = 'PAYMENT_PENDING'
                )
            """)
    List<Booking> findBookingsForDate(
            @Param("salonId") Long salonId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("statuses") List<String> statuses
    );

    Optional<Booking> findFirstByCustomerIdAndSalonIdAndStartTimeAndStatus(
            Long customerId,
            Long salonId,
            LocalDateTime startTime,
            String status
    );

    // Returns the count of active bookings that overlap with the requested time slot for resource-based availability validation
    @Query("""
             SELECT COUNT(b)
             FROM Booking b
             WHERE b.salonId = :salonId
             AND b.startTime < :end
             AND b.endTime > :start
             AND (
                 b.status IN :statuses
                 OR (
                     b.status = 'PAYMENT_PENDING'
                     AND b.createdDate >= :pendingCutoff
                 )
             )
            """)
    long countOverlappingBookings(
            @Param("salonId") Long salonId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<String> statuses,
            @Param("pendingCutoff") LocalDateTime pendingCutoff
    );

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.salonId = :salonId
        AND b.startTime < :end
        AND b.endTime > :start
        AND (
            b.status IN :statuses
            OR b.status = 'PAYMENT_PENDING'
        )
       """)
    List<Booking> findOverlappingBookings(
            @Param("salonId") Long salonId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<String> statuses
    );

    // COUPON ELIGIBILITY (Section A)

    // Total completed bookings for a customer — used for minimumBookingCount / maximumBookingCount
    // and for the FIRST_BOOKING / RETURNING_CUSTOMERS targetType checks.
    long countByCustomerIdAndStatus(Long customerId, String status);

    // Has this customer completed at least one booking with the given partner? Booking only stores
    // salonId, so this joins through SalonDetails to reach the owning partner — used for
    // targetType = PARTNER_CUSTOMERS / existingPartnerCustomerOnly.
    @Query("""
                SELECT COUNT(b) > 0
                FROM Booking b
                JOIN SalonDetails s ON s.salonId = b.salonId
                WHERE b.customerId = :customerId
                AND s.partner.partnerId = :partnerId
                AND b.status = 'COMPLETED'
            """)
    boolean existsCompletedBookingByCustomerIdAndPartnerId(
            @Param("customerId") Long customerId,
            @Param("partnerId") Long partnerId
    );

    // Most recent completed-booking date for a customer — used to identify "returning" /
    // lapsed customers (i.e. had bookings before, but nothing recent) for targetType = RETURNING_CUSTOMERS.
    @Query("""
                SELECT MAX(b.createdDate)
                FROM Booking b
                WHERE b.customerId = :customerId
                AND b.status = 'COMPLETED'
            """)
    Optional<LocalDateTime> findLastCompletedBookingDate(@Param("customerId") Long customerId);
}