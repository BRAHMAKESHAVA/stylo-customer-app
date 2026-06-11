package org.backend.repository;

import org.backend.enums.BookingStatus;
import org.backend.model.Booking;
import org.backend.model.BookingApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingApprovalRepository extends JpaRepository<BookingApproval, Long> {

    List<BookingApproval> findByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    List<BookingApproval> findByApprovalStatusAndCreatedAtBefore(
            String approvalStatus,
            LocalDateTime cutoff
    );

}