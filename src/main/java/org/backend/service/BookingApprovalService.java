package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.partner.BookingApprovalRequest;
import org.backend.dto.partner.BookingApprovalResponse;
import org.backend.dto.partner.BookingApprovalUpdateRequest;
import org.backend.model.BookingApproval;
import org.backend.repository.BookingApprovalRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingApprovalService {

    private final BookingApprovalRepository approvalRepository;
    private final PartnerWebSocketService partnerWebSocketService;
    private final NotificationService notificationService;


    public BookingApprovalResponse createApproval(Long salonId, BookingApprovalRequest request) {

        BookingApproval approval = BookingApproval.builder()
                .customerId(request.getCustomerId())
                .serviceDuration(request.getServiceDuration())
                .slotDate(request.getSlotDate())
                .slotStartTime(request.getSlotStartTime())
                .slotEndTime(request.getSlotEndTime())
                .workingEndTime(request.getWorkingEndTime())
                .bookingId(null)
                .remarks(null)
                .build();

        BookingApproval saved = approvalRepository.save(approval);
        BookingApprovalResponse response =  buildResponse(saved);

        // Real-time dashboard update
        partnerWebSocketService.notifyPartner(response);

        // Mobile push notification
        notificationService.sendBooingApprovalToPartner(response.getCustomerId(), response);

//        notificationService.sendToPartner(
//                response.getCustomerId(),
//                "Booking Approval Required",
//                "A new booking request from customer #" + request.getCustomerId()
//                        + " requires your approval."
//        );

        return response;
    }

    public BookingApprovalResponse updateApproval(Long approvalId, BookingApprovalUpdateRequest request) {
        BookingApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("Booking approval not found"));

        if (request.getCustomerId() != null) {
            approval.setCustomerId(request.getCustomerId());
        }
        if (request.getServiceDuration() != null) {
            approval.setServiceDuration(request.getServiceDuration());
        }
        if (request.getSlotDate() != null) {
            approval.setSlotDate(request.getSlotDate());
        }
        if (request.getSlotStartTime() != null) {
            approval.setSlotStartTime(request.getSlotStartTime());
        }
        if (request.getWorkingEndTime() != null) {
            approval.setWorkingEndTime(request.getWorkingEndTime());
        }
        if (request.getApprovalStatus() != null) {
            approval.setApprovalStatus(request.getApprovalStatus());
        }
        if (request.getRemarks() != null) {
            approval.setRemarks(request.getRemarks());
        }

        BookingApproval saved = approvalRepository.save(approval);
        BookingApprovalResponse response = buildResponse(saved);
        partnerWebSocketService.notifyCustomer(
                approval.getCustomerId(),
                response
        );

        return response;
    }

    public BookingApprovalResponse getBookingApproval(Long approvalId) {

        BookingApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() ->
                        new RuntimeException("Booking approval not found"));

        return buildResponse(approval);
    }

    // Reusable mapper method
    private BookingApprovalResponse buildResponse(BookingApproval approval) {
        return BookingApprovalResponse.builder()
                .approvalId(approval.getApprovalId())
                .customerId(approval.getCustomerId())
                .bookingId(approval.getBookingId())
                .serviceDuration(approval.getServiceDuration())
                .slotDate(approval.getSlotDate())
                .slotStartTime(approval.getSlotStartTime())
                .slotEndTime(approval.getSlotEndTime())
                .workingEndTime(approval.getWorkingEndTime())
                .approvalStatus(approval.getApprovalStatus())
                .remarks(approval.getRemarks())
                .createdAt(approval.getCreatedAt())
                .build();
    }
}
