package org.backend.repository;

import org.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByProviderPaymentId(String razorpayOrderId);

    List<Payment> findByBookingIdIn(List<Long> bookingIds);

    /**
     * Find all payments with a given status created before a cutoff time.
     * Used by reconciliation scheduler to detect stale INITIATED payments.
     */
    List<Payment> findByStatusAndCreatedDateBefore(String status, LocalDateTime cutoffTime);



}