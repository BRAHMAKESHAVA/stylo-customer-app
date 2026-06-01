package org.backend.repository;

import jakarta.transaction.Transactional;
import org.backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingIdAndSalonId(
            Long bookingId,
            Long salonId
    );

    List<Booking> findBySalonIdAndStatus(
            Long salonId,
            String status
    );

    List<Booking> findByStatus(String status);

    List<Booking> findByStatusOrderByCreatedDateDesc(String status);


    @Query("SELECT b FROM Booking b WHERE b.customerId = ?1 AND b.status <> 'PENDING'")
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
                 OR (
                     b.status = 'PAYMENT_PENDING'
                     AND b.createdDate >= :pendingCutoff
                 )
             )
            """)
    List<Booking> findBookingsForDate(
            @Param("salonId") Long salonId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("statuses") List<String> statuses,
            @Param("pendingCutoff") LocalDateTime pendingCutoff
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
}