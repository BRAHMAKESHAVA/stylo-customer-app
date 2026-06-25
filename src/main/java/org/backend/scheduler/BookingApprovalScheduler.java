package org.backend.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.enums.BookingStatus;
import org.backend.model.BookingApproval;
import org.backend.repository.BookingApprovalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingApprovalScheduler {

    private final BookingApprovalRepository bookingApprovalRepository;

    @Scheduled(fixedRate = 5000) // every 5 seconds
    @Transactional
    public void autoExpirePartnerApproval() {
        System.out.println("Checking for expired booking approvals...");

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1); // configurable timeout

        // Fetch only approvals that are still pending and older than cutoff
        List<BookingApproval> expiredApprovals =
                bookingApprovalRepository.findByApprovalStatusAndCreatedAtBefore(
                        BookingStatus.PENDING_PARTNER_CONFIRMATION.name(), cutoff);

        if (expiredApprovals.isEmpty()) {
            System.out.println("No expired approvals found.");
            return;
        }

        expiredApprovals.forEach(approval -> {
            approval.setApprovalStatus(BookingStatus.APPROVAL_NO_SHOW.name());
            approval.setRemarks("System auto-cancelled as partner did not respond in time");
        });

        bookingApprovalRepository.saveAll(expiredApprovals);
        System.out.println("Marked " + expiredApprovals.size() + " approvals as APPROVAL_NO_SHOW");
    }
}
