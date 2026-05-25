package org.backend.controller;

import lombok.RequiredArgsConstructor;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.partner.PartnerBookingPendingResponseDTO;
import org.backend.dto.partner.PartnerBookingStatusResponseDTO;
import org.backend.dto.partner.PartnerBookingStatusUpdateRequestDTO;
import org.backend.service.PartnerBookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partner/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PartnerBookingController {

    private final PartnerBookingService partnerBookingService;

    /*
     * GET PENDING BOOKINGS
     */
    @GetMapping("/pending")
    public ApiResponseDTO<List<PartnerBookingPendingResponseDTO>> getPendingBookings() {

        List<PartnerBookingPendingResponseDTO> response =
                partnerBookingService.getPendingBookings();

        return ApiResponseDTO.<List<PartnerBookingPendingResponseDTO>>builder()
                .status(true)
                .message("Pending bookings fetched successfully")
                .data(response)
                .build();
    }

    /*
     * GET BOOKINGS BY STATUS
     */
    @GetMapping
    public ApiResponseDTO<List<PartnerBookingPendingResponseDTO>> getBookingsByStatus(
            @RequestParam String status
    ) {

        List<PartnerBookingPendingResponseDTO> response =
                partnerBookingService.getBookingsByStatus(status);

        return ApiResponseDTO.<List<PartnerBookingPendingResponseDTO>>builder()
                .status(true)
                .message("Bookings fetched successfully")
                .data(response)
                .build();
    }

    /*
     * CONFIRM / REJECT
     */
    @PutMapping("/{bookingId}/status")
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestBody PartnerBookingStatusUpdateRequestDTO req
    ) {

        PartnerBookingStatusResponseDTO response =
                partnerBookingService.updateBookingStatus(
                        bookingId,
                        req
                );

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Booking updated successfully")
                .data(response)
                .build();
    }

    /*
     * PAYMENT COLLECTED
     */
    @PutMapping("/{bookingId}/payment-collected")
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> markPaymentCollected(@PathVariable Long bookingId
    ) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.markPaymentCollected(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Payment collected successfully")
                .data(response)
                .build();
    }

    /*
     * START SERVICE
     */
    @PutMapping("/{bookingId}/start")
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> startService(@PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.startService(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Service started successfully")
                .data(response)
                .build();
    }

    /*
     * COMPLETE SERVICE
     */
    @PutMapping("/{bookingId}/complete")
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> completeService(@PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.completeService(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Service completed successfully")
                .data(response)
                .build();
    }

    /*
     * NO SHOW
     */
    @PutMapping("/{bookingId}/no-show")
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> markNoShow(@PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.markNoShow(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Booking marked as no-show")
                .data(response)
                .build();
    }
}