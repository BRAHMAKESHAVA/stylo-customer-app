package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.RefundResultDTO;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.partner.BookingApprovalResponse;
import org.backend.dto.partner.PartnerBookingPendingResponseDTO;
import org.backend.dto.partner.PartnerBookingStatusResponseDTO;
import org.backend.dto.partner.PartnerBookingStatusUpdateRequestDTO;
import org.backend.enums.BookingServiceStatus;
import org.backend.enums.BookingStatus;
import org.backend.enums.PaymentMode;
import org.backend.enums.PaymentStatus;
import org.backend.exception.BadRequestException;
import org.backend.model.Booking;
import org.backend.model.BookingApproval;
import org.backend.model.BookingServiceEntity;
import org.backend.model.Payment;
import org.backend.repository.BookingApprovalRepository;
import org.backend.repository.BookingRepository;
import org.backend.repository.BookingServiceRepository;
import org.backend.repository.PaymentRepository;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerBookingService {

    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final BookingApprovalRepository approvalRepository;
    private final PartnerWebSocketService partnerWebSocketService;
    private final BookingService bookingService;

    public List<BookingApprovalResponse> getBookingsForApproval() {

        List<BookingApproval> bookings =
                approvalRepository.findByApprovalStatusOrderByCreatedAtDesc(
                        BookingStatus.PENDING_PARTNER_CONFIRMATION.name()
                );

        System.out.println("Records found: " + bookings.size());
        return bookings.stream()
                .map(this::buildResponse)
                .toList();
    }

    /*
     * GET PENDING BOOKINGS
     */
    public List<PartnerBookingPendingResponseDTO> getPendingBookings() {

        // Partner should only see bookings waiting for approval
        List<Booking> bookings =
                bookingRepository.findByStatusOrderByCreatedDateDesc(
                        BookingStatus.PENDING_PARTNER_CONFIRMATION.name()
                );

        return bookings.stream()
                .map(this::mapToPendingDTO)
                .collect(Collectors.toList());
    }

    /*
     * GET BOOKINGS BY STATUS
     */
    public List<PartnerBookingPendingResponseDTO> getBookingsByStatus(String status) {

        List<Booking> bookings =
                bookingRepository.findByStatusOrderByCreatedDateDesc(status);

        return bookings.stream()
                .map(this::mapToPendingDTO)
                .collect(Collectors.toList());
    }

    /*
     * CONFIRM / REJECT
     */
    @Transactional
    public PartnerBookingStatusResponseDTO updateBookingStatus(
            UUID bookingId,
            PartnerBookingStatusUpdateRequestDTO req
    ) {

        Booking booking = getBooking(bookingId);
        Payment payment = getPayment(bookingId);

        // only pending partner approval bookings can be processed
        if (!BookingStatus.PENDING_PARTNER_CONFIRMATION.name().equals(booking.getStatus())) {
            throw new BadRequestException("Booking is not pending partner confirmation");
        }

        String status = req.getStatus();

        // If confirming, just update booking status
        if ("CONFIRMED".equalsIgnoreCase(status)) {
            booking.setStatus(BookingStatus.CONFIRMED.name());
            booking.setUpdatedDate(LocalDateTime.now());

            bookingRepository.save(booking);

            // Notify customer about confirmation via WebSocket
            BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
            partnerWebSocketService.notifyCustomer(
                    booking.getCustomerId(),
                    bookingResponse
            );

            return PartnerBookingStatusResponseDTO.builder()
                    .bookingId(booking.getBookingId())
                    .bookingStatus(booking.getStatus())
                    .paymentStatus(payment != null ? payment.getStatus() : null)
                    .message("Booking confirmed successfully")
                    .build();
        }

        // REJECT
        if ("REJECTED".equalsIgnoreCase(status)) {

            if (req.getReason() == null || req.getReason().trim().isEmpty()) {
                throw new RuntimeException("Rejection reason is required");
            }

            booking.setStatus(BookingStatus.REJECTED.name());
            booking.setRejectionReason(req.getReason());
            booking.setUpdatedDate(LocalDateTime.now());

            bookingRepository.save(booking);

            // Notify customer about rejection via WebSocket
            BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
            partnerWebSocketService.notifyCustomer(
                    booking.getCustomerId(),
                    bookingResponse
            );

            // cancel all booking services
            List<BookingServiceEntity> services = bookingServiceRepository.findByBookingId(bookingId);

            services.forEach(service -> service.setStatus(BookingServiceStatus.CANCELLED.name()));

            bookingServiceRepository.saveAll(services);

            //BigDecimal refundAmount = BigDecimal.ZERO;
            //String paymentStatus = null;

            // Refund only for online payment
            if ("RAZORPAY".equalsIgnoreCase(payment.getProvider())) {

                RefundResultDTO refundResult = paymentService.refundPayment(
                        bookingId, req.getReason());

                return PartnerBookingStatusResponseDTO.builder()
                        .bookingId(booking.getBookingId())
                        .bookingStatus(booking.getStatus())
                        .rejectionReason(booking.getRejectionReason())
                        .paymentStatus(refundResult.getPaymentStatus())
                        .refundAmount(refundResult.getRefundAmount())
                        .message("Booking rejected and refund processed")
                        .build();
            }

            // Cancel payment record for pay at salon rejection
            if (PaymentMode.OFFLINE.name()
                    .equalsIgnoreCase(payment.getProvider())) {

                payment.setStatus(PaymentStatus.CANCELLED.name());
                payment.setUpdatedDate(LocalDateTime.now());

                paymentRepository.save(payment);

                return PartnerBookingStatusResponseDTO.builder()
                        .bookingId(booking.getBookingId())
                        .bookingStatus(booking.getStatus())
                        .rejectionReason(booking.getRejectionReason())
                        .paymentStatus(payment.getStatus())
                        .message("Booking rejected successfully")
                        .build();
            }
        }

        throw new RuntimeException("Invalid booking status");
    }

    /*
     * PAY AT SALON PAYMENT COLLECTED
     */
    @Transactional
    public PartnerBookingStatusResponseDTO markPaymentCollected(UUID bookingId) {

        Booking booking = getBooking(bookingId);
        Payment payment = getPayment(bookingId);

        // Pay-at-salon collection only
        if (!PaymentMode.OFFLINE.name().equalsIgnoreCase(payment.getProvider())) {
            throw new RuntimeException("Payment collection only allowed for OFFLINE/PAY_AT_SALON bookings");
        }

        // Prevent duplicate payment collection
        if (PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            throw new RuntimeException("Payment already collected");
        }

        // mark payment successful
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setUpdatedDate(LocalDateTime.now());

        paymentRepository.save(payment);

        return PartnerBookingStatusResponseDTO.builder()
                .bookingId(bookingId)
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment.getStatus())
                .message("Payment collected successfully")
                .build();
    }

    /*
     * START SERVICE
     */
    @Transactional
    public PartnerBookingStatusResponseDTO startService(UUID bookingId) {

        Booking booking = getBooking(bookingId);
        Payment payment = getPayment(bookingId);

        // only confirmed bookings can start
        if (!BookingStatus.CONFIRMED.name().equals(booking.getStatus())) {
            throw new RuntimeException("Only confirmed bookings can be started");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS.name());
        booking.setUpdatedDate(LocalDateTime.now());

        // mark all services in progress
        List<BookingServiceEntity> services = getBookingServices(bookingId);

        services.forEach(service ->
                service.setStatus(BookingServiceStatus.IN_PROGRESS.name())
        );

        bookingServiceRepository.saveAll(services);
        bookingRepository.save(booking);

        // Notify customer about service start via WebSocket
        BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
        partnerWebSocketService.notifyCustomer(
                booking.getCustomerId(),
                bookingResponse
        );

        return PartnerBookingStatusResponseDTO.builder()
                .bookingId(bookingId)
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .message("Service started successfully")
                .build();
    }

    /*
     * COMPLETE SERVICE
     */
    @Transactional
    public PartnerBookingStatusResponseDTO completeService(UUID bookingId) {

        Booking booking = getBooking(bookingId);
        Payment payment = getPayment(bookingId);

        // only in-progress bookings can complete
        if (!BookingStatus.IN_PROGRESS.name().equals(booking.getStatus())) {
            throw new BadRequestException("Only in-progress bookings can be completed");
        }

        // PAY_AT_SALON must be collected before completion
        if (PaymentMode.OFFLINE.name().equalsIgnoreCase(payment.getProvider())
                && !PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            throw new BadRequestException(
                    "Please collect payment before completing service"
            );
        }

        booking.setStatus(BookingStatus.COMPLETED.name());
        booking.setUpdatedDate(LocalDateTime.now());

        // mark all services completed
        List<BookingServiceEntity> services = getBookingServices(bookingId);
        services.forEach(service ->
                service.setStatus(BookingServiceStatus.COMPLETED.name())
        );

        bookingServiceRepository.saveAll(services);
        bookingRepository.save(booking);

        // Notify customer about service completion via WebSocket
        BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
        partnerWebSocketService.notifyCustomer(
                booking.getCustomerId(),
                bookingResponse
        );

        return PartnerBookingStatusResponseDTO.builder()
                .bookingId(bookingId)
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .message("Service completed successfully")
                .build();
    }

    /*
     * NO SHOW
     */
    @Transactional
    public PartnerBookingStatusResponseDTO markNoShow(UUID bookingId) {

        Booking booking = getBooking(bookingId);
        Payment payment = getPayment(bookingId);

        booking.setStatus(BookingStatus.NO_SHOW.name());
        booking.setUpdatedDate(LocalDateTime.now());

        bookingRepository.save(booking);

        // Notify customer about no show status via WebSocket
        BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
        partnerWebSocketService.notifyCustomer(
                booking.getCustomerId(),
                bookingResponse
        );

        // cancel all services
        List<BookingServiceEntity> services = getBookingServices(bookingId);
        services.forEach(service ->
                service.setStatus(BookingServiceStatus.CANCELLED.name())
        );
        bookingServiceRepository.saveAll(services);

        // PAY_AT_SALON no show => cancel payment
        if (payment != null && PaymentMode.OFFLINE.name()
                .equalsIgnoreCase(payment.getProvider())) {

            payment.setStatus(PaymentStatus.CANCELLED.name());
            payment.setUpdatedDate(LocalDateTime.now());

            paymentRepository.save(payment);
        }

        return PartnerBookingStatusResponseDTO.builder()
                .bookingId(bookingId)
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .message("Booking marked as no show")
                .build();
    }

    /*
     * DTO MAPPING
     */
    private PartnerBookingPendingResponseDTO mapToPendingDTO(Booking booking) {

        List<BookingServiceEntity> services = getBookingServices(booking.getBookingId());
        Payment payment = getPayment(booking.getBookingId());

        List<String> serviceNames =
                services.stream()
                        .map(BookingServiceEntity::getServiceName)
                        .collect(Collectors.toList());

        return PartnerBookingPendingResponseDTO.builder()
                .bookingId(booking.getBookingId())
                .customerId(booking.getCustomerId())
                .salonId(booking.getSalonId())
                .services(serviceNames)
                .packageName(
                        booking.getPackageId() != null
                                ? "Package #" + booking.getPackageId()
                                : null
                )
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .paymentMode(payment != null ? payment.getProvider() : "UNKNOWN")
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .finalAmount(booking.getFinalAmount())
                .bookingStatus(booking.getStatus())
                .build();
    }

    /*
     * Helper methods to fetch booking, payment and services
     */
    private Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));
    }

    private Payment getPayment(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));
    }

    private List<BookingServiceEntity> getBookingServices(UUID bookingId) {
        return bookingServiceRepository.findByBookingId(bookingId);
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
                .workingEndTime(approval.getWorkingEndTime())
                .approvalStatus(approval.getApprovalStatus())
                .remarks(approval.getRemarks())
                .createdAt(approval.getCreatedAt())
                .build();
    }
}