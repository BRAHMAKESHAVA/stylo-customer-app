package org.backend.repository;

import org.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findByBookingId(UUID bookingId);

    boolean existsByProviderPaymentId(String razorpayOrderId);

    List<Payment> findByBookingIdIn(List<UUID> bookingIds);

    /**
     * Find all payments with a given status created before a cutoff time.
     * Used by reconciliation scheduler to detect stale INITIATED payments.
     */
    List<Payment> findByStatusAndCreatedDateBefore(String status, LocalDateTime cutoffTime);

    // Returns booking IDs where payment was INITIATED within the hold window
    @Query("""
                SELECT p.bookingId
                FROM Payment p
                WHERE p.status = 'INITIATED'
                AND p.createdDate >= :holdThreshold
            """)
    List<UUID> findActiveHoldBookingIds(@Param("holdThreshold") LocalDateTime holdThreshold);


}