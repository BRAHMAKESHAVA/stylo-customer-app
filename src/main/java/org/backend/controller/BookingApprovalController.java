package org.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.partner.BookingApprovalRequest;
import org.backend.dto.partner.BookingApprovalResponse;
import org.backend.dto.partner.BookingApprovalUpdateRequest;
import org.backend.enums.BookingMode;
import org.backend.service.BookingApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/partner/salon")
public class BookingApprovalController {

    private final BookingApprovalService bookingApprovalService;

    @PostMapping("/{salonId}/booking-mode")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getBookingMode(
            @PathVariable Long salonId,
            @RequestBody Map<String, BookingMode> request) {

        BookingMode requestedMode = request.get("mode");

        BookingMode bookingMode = (requestedMode == BookingMode.ANY)
                ? BookingMode.AUTOMATIC
                : requestedMode;

        Map<String, Object> data = new HashMap<>();
        data.put("bookingMode", bookingMode);

        if (bookingMode == BookingMode.MANUAL) {
            data.put("waitingTimeMinutes", 1);
        }

        return ResponseEntity.ok(
                ApiResponseDTO.<Map<String, Object>>builder()
                        .status(true)
                        .message("Booking mode fetched successfully")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/{salonId}/booking-approval")
    public ResponseEntity<ApiResponseDTO<BookingApprovalResponse>> approveBooking(
            @PathVariable Long salonId,
            @Valid @RequestBody BookingApprovalRequest request) {

        BookingApprovalResponse approval =
                bookingApprovalService.createApproval(salonId, request);

        ApiResponseDTO<BookingApprovalResponse> response = ApiResponseDTO.<BookingApprovalResponse>builder()
                .status(true)
                .message("Booking approval created successfully")
                .data(approval)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/booking-approval/{approvalId}")
    public ResponseEntity<ApiResponseDTO<BookingApprovalResponse>> updateBookingApproval(
            @PathVariable Long approvalId,
            @RequestBody BookingApprovalUpdateRequest request) {

        BookingApprovalResponse approval =
                bookingApprovalService.updateApproval(approvalId, request);

        ApiResponseDTO<BookingApprovalResponse> response =
                ApiResponseDTO.<BookingApprovalResponse>builder()
                        .status(true)
                        .message("Booking approval updated successfully")
                        .data(approval)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking-approval/{approvalId}")
    public ResponseEntity<ApiResponseDTO<BookingApprovalResponse>> getBookingApproval(
            @PathVariable Long approvalId) {

        BookingApprovalResponse approval =
                bookingApprovalService.getBookingApproval(approvalId);

        ApiResponseDTO<BookingApprovalResponse> response =
                ApiResponseDTO.<BookingApprovalResponse>builder()
                        .status(true)
                        .message("Booking approval fetched successfully")
                        .data(approval)
                        .build();

        return ResponseEntity.ok(response);
    }

}
