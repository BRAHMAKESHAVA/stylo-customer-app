package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.partner.PartnerBookingPendingResponseDTO;
import org.backend.dto.partner.PartnerBookingStatusResponseDTO;
import org.backend.dto.partner.PartnerBookingStatusUpdateRequestDTO;
import org.backend.service.PartnerBookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for partner-side booking management.
 * Provides endpoints for partners to view, confirm, reject, and manage
 * the full lifecycle of bookings at their salon.
 * All endpoints operate under the base path "/api/partner/bookings".
 */
@RestController
@RequestMapping("/api/partner/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(
        name = "Partner Booking Management",
        description = "Endpoints for salon partners to manage bookings — including approval, " +
                "rejection, service lifecycle, payment collection, and no-show handling"
)
public class PartnerBookingController {

    private final PartnerBookingService partnerBookingService;

    /**
     * Retrieves all bookings awaiting partner confirmation.
     * Only returns bookings in PENDING_PARTNER_CONFIRMATION status,
     * ordered by creation date descending.
     *
     * @return list of PartnerBookingPendingResponseDTO for pending bookings
     */
    @GetMapping("/pending")
    @Operation(
            summary = "Get all pending bookings",
            description = """
                    Retrieves all bookings currently awaiting partner approval
                    (status: PENDING_PARTNER_CONFIRMATION), ordered by creation date descending.
                    Each entry includes customer ID, services, start/end times,
                    payment mode, payment status, and final amount.
                    """,
            operationId = "getPendingBookings"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pending bookings fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<List<PartnerBookingPendingResponseDTO>> getPendingBookings() {

        List<PartnerBookingPendingResponseDTO> response =
                partnerBookingService.getPendingBookings();

        return ApiResponseDTO.<List<PartnerBookingPendingResponseDTO>>builder()
                .status(true)
                .message("Pending bookings fetched successfully")
                .data(response)
                .build();
    }

    /**
     * Retrieves bookings filtered by a given status.
     * Ordered by creation date descending.
     *
     * @param status the booking status to filter by (e.g. CONFIRMED, COMPLETED, REJECTED)
     * @return list of PartnerBookingPendingResponseDTO for the given status
     */
    @GetMapping
    @Operation(
            summary = "Get bookings by status",
            description = """
                    Retrieves all bookings for the partner filtered by the provided status,
                    ordered by creation date descending.
                    Valid status values: PAYMENT_PENDING, PENDING_PARTNER_CONFIRMATION,
                    CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED, NO_SHOW, PAYMENT_FAILED.
                    """,
            operationId = "getBookingsByStatus"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bookings fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<List<PartnerBookingPendingResponseDTO>> getBookingsByStatus(
            @Parameter(description = "Booking status to filter by (e.g. CONFIRMED, COMPLETED, REJECTED)")
            @RequestParam String status) {

        List<PartnerBookingPendingResponseDTO> response =
                partnerBookingService.getBookingsByStatus(status);

        return ApiResponseDTO.<List<PartnerBookingPendingResponseDTO>>builder()
                .status(true)
                .message("Bookings fetched successfully")
                .data(response)
                .build();
    }

    /**
     * Confirms or rejects a booking pending partner approval.
     * On CONFIRM: booking advances to CONFIRMED status.
     * On REJECT: booking services are cancelled, and a refund is issued
     * for RAZORPAY payments or the payment record is cancelled for OFFLINE bookings.
     *
     * @param bookingId the unique identifier of the booking
     * @param req       request body containing status ("CONFIRMED" or "REJECTED") and optional reason
     * @return PartnerBookingStatusResponseDTO with updated booking and payment state
     */
    @PutMapping("/{bookingId}/status")
    @Operation(
            summary = "Confirm or reject a booking",
            description = """
                    Updates the status of a booking pending partner confirmation.
                    - CONFIRMED: advances booking to CONFIRMED status.
                    - REJECTED: requires a rejection reason; cancels all associated services.
                      For RAZORPAY payments, a refund is automatically initiated.
                      For OFFLINE (Pay-at-Salon) payments, the payment record is cancelled.
                    Only bookings in PENDING_PARTNER_CONFIRMATION status can be processed.
                    """,
            operationId = "updateBookingStatus"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Booking is not pending partner confirmation"
                            - "Rejection reason is required" (when status is REJECTED and reason is blank)
                            - "Invalid booking status" (status is neither CONFIRMED nor REJECTED)
                            - "Payment is not successful" (refund prerequisite failure)
                            - "Refund already initiated"
                            - "Payment already refunded"
                            - "No refund applicable"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found"
                            - "Payment not found"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Razorpay gateway error — refund request failed due to an upstream error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> updateBookingStatus(
            @Parameter(description = "Unique identifier of the booking to confirm or reject")
            @PathVariable Long bookingId,
            @Parameter(description = "Request body with status ('CONFIRMED' or 'REJECTED') and optional rejection reason")
            @RequestBody PartnerBookingStatusUpdateRequestDTO req) {

        PartnerBookingStatusResponseDTO response =
                partnerBookingService.updateBookingStatus(bookingId, req);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Booking updated successfully")
                .data(response)
                .build();
    }

    /**
     * Records that cash payment has been collected from the customer at the salon.
     * Only applicable for OFFLINE (Pay-at-Salon) bookings.
     * Marks the payment as SUCCESS.
     *
     * @param bookingId the unique identifier of the booking
     * @return PartnerBookingStatusResponseDTO with updated payment state
     */
    @PutMapping("/{bookingId}/payment-collected")
    @Operation(
            summary = "Mark Pay-at-Salon payment as collected",
            description = """
                    Records that the partner has physically collected cash payment from the customer.
                    Only applicable for OFFLINE (Pay-at-Salon) bookings.
                    Marks the associated payment record as SUCCESS.
                    Must be called before completing the service for Pay-at-Salon bookings.
                    """,
            operationId = "markPaymentCollected"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment collected successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Payment collection only allowed for OFFLINE/PAY_AT_SALON bookings"
                            - "Payment already collected"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found"
                            - "Payment not found"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> markPaymentCollected(
            @Parameter(description = "Unique identifier of the booking for which payment was collected")
            @PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.markPaymentCollected(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Payment collected successfully")
                .data(response)
                .build();
    }

    /**
     * Marks a confirmed booking as IN_PROGRESS and updates all associated services accordingly.
     * Should be called when the partner begins the service for the customer.
     *
     * @param bookingId the unique identifier of the booking
     * @return PartnerBookingStatusResponseDTO with updated booking state
     */
    @PutMapping("/{bookingId}/start")
    @Operation(
            summary = "Start service for a confirmed booking",
            description = """
                    Advances a CONFIRMED booking to IN_PROGRESS status and marks all associated
                    services as IN_PROGRESS. Should be called by the partner when they begin
                    serving the customer at the salon.
                    Only bookings in CONFIRMED status can be started.
                    """,
            operationId = "startService"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service started successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Only confirmed bookings can be started"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found"
                            - "Payment not found"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> startService(
            @Parameter(description = "Unique identifier of the booking to start")
            @PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.startService(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Service started successfully")
                .data(response)
                .build();
    }

    /**
     * Marks an in-progress booking as COMPLETED and updates all associated services accordingly.
     * For Pay-at-Salon bookings, payment must be collected before completion is allowed.
     *
     * @param bookingId the unique identifier of the booking
     * @return PartnerBookingStatusResponseDTO with updated booking state
     */
    @PutMapping("/{bookingId}/complete")
    @Operation(
            summary = "Complete an in-progress service",
            description = """
                    Marks an IN_PROGRESS booking as COMPLETED and updates all associated services
                    to COMPLETED status. Should be called by the partner when the service is finished.
                    For Pay-at-Salon (OFFLINE) bookings, payment must be marked as collected
                    via /payment-collected before this endpoint can be called.
                    Only bookings in IN_PROGRESS status can be completed.
                    """,
            operationId = "completeService"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service completed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Only in-progress bookings can be completed"
                            - "Please collect payment before completing service" (OFFLINE payment not yet collected)
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found"
                            - "Payment not found"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> completeService(
            @Parameter(description = "Unique identifier of the booking to complete")
            @PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.completeService(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Service completed successfully")
                .data(response)
                .build();
    }

    /**
     * Marks a booking as NO_SHOW when the customer does not arrive.
     * Cancels all associated services.
     * For OFFLINE bookings, the payment record is also cancelled.
     *
     * @param bookingId the unique identifier of the booking
     * @return PartnerBookingStatusResponseDTO with updated booking state
     */
    @PutMapping("/{bookingId}/no-show")
    @Operation(
            summary = "Mark a booking as no-show",
            description = """
                    Records that the customer did not arrive for their booking.
                    Updates booking status to NO_SHOW and cancels all associated services.
                    For OFFLINE (Pay-at-Salon) bookings, the payment record is also cancelled.
                    For RAZORPAY bookings, no automatic refund is triggered — handle separately if needed.
                    """,
            operationId = "markNoShow"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking marked as no-show successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found"
                            - "Payment not found"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<PartnerBookingStatusResponseDTO> markNoShow(
            @Parameter(description = "Unique identifier of the booking to mark as no-show")
            @PathVariable Long bookingId) {

        PartnerBookingStatusResponseDTO response = partnerBookingService.markNoShow(bookingId);

        return ApiResponseDTO.<PartnerBookingStatusResponseDTO>builder()
                .status(true)
                .message("Booking marked as no-show")
                .data(response)
                .build();
    }
}