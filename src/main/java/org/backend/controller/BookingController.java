package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.booking.BookingRequestDTO;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.AvailableSlotsRequest;
import org.backend.dto.request.PriceSummaryRequestDTO;
import org.backend.dto.response.SlotResponseDTO;
import org.backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing salon bookings.
 * Provides endpoints for creating bookings, checking slot availability,
 * fetching price summaries, retrieving customer bookings, and cancellations.
 * All endpoints operate under the base path "/api/bookings".
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Tag(
        name = "Booking Management",
        description = "Endpoints for creating and managing salon bookings, slot availability, and pricing"
)
public class BookingController {

    private final BookingService bookingService;

    /**
     * Creates a new booking for a customer.
     * Booking is always created in PAYMENT_PENDING status.
     * Supports both package-based and add-on service bookings.
     *
     * @param bookingReq the booking request payload
     * @return BookingResponseDTO containing the created booking details
     */
    @PostMapping("/create-booking")
    @Operation(
            summary = "Create a new booking",
            description = """
                    Creates a new booking for a customer at a salon. Booking is always created
                    in PAYMENT_PENDING status. Supports package bookings, add-on service bookings,
                    or a combination of both. Includes idempotency check to prevent duplicate
                    PAYMENT_PENDING bookings for the same slot within the hold window.
                    """,
            operationId = "createBooking"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Salon is required"
                            - "Booking time is required"
                            - "Please select at least one service or package"
                            - "Selected package is not available for this salon"
                            - "Selected package has no services configured"
                            - "Package contains invalid services"
                            - "One or more selected services are invalid"
                            - "Some selected services are unavailable for this salon"
                            - "Selected slot is no longer available"
                            - "Salon not found"
                            - "Salon resource config not found"
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
    public BookingResponseDTO createBooking(
            @Parameter(description = "Booking request payload containing salonId, customerId, startTime, packageId (optional), and serviceIds (optional)")
            @Valid @RequestBody BookingRequestDTO bookingReq) {
        return bookingService.createBooking(bookingReq);
    }

    /**
     * Retrieves all bookings for a given customer.
     * Enriches each booking with salon name, payment provider, and refund preview.
     *
     * @param customerId the unique identifier of the customer
     * @return list of BookingResponseDTO with booking and related details
     */
    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Get all bookings for a customer",
            description = """
                    Retrieves all bookings associated with a customer, enriched with salon details,
                    payment provider information, and a refund eligibility preview.
                    Refund tiers: FULL (within 10 min of payment), PARTIAL (10–60 min), NONE (after 60 min).
                    """,
            operationId = "getCustomerBookings"
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
    public ApiResponseDTO<List<BookingResponseDTO>> getUserBookings(
            @Parameter(description = "Unique identifier of the customer")
            @PathVariable Long customerId) {
        return ApiResponseDTO.<List<BookingResponseDTO>>builder()
                .status(true)
                .message("Bookings fetched successfully")
                .data(bookingService.getCustomerBookings(customerId))
                .build();
    }

    /**
     * Returns available time slots for a salon on a given date based on selected services.
     * Each slot is tagged with a status: AVAILABLE, BOOKED, HOLD, UNAVAILABLE, or PAST.
     *
     * @param request the slot availability request containing salonId, serviceIds, and date
     * @return list of SlotResponseDTO with time and status for each slot
     */
    @PostMapping("/available-slots")
    @Operation(
            summary = "Get available slots for a salon",
            description = """
                    Returns time slots for a salon on the specified date, based on the total duration
                    of the requested services. Each slot is tagged with one of the following statuses:
                    - AVAILABLE: slot is open for booking
                    - BOOKED: slot is fully occupied by confirmed/in-progress bookings
                    - HOLD: slot is held by a PAYMENT_PENDING booking within the hold window
                    - UNAVAILABLE: an existing booking starts within the service window, making it unfit
                    - PAST: slot has already passed (only applicable for today's date)
                    """,
            operationId = "getAvailableSlots"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Slots fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Services not found"
                            - "Salon not found"
                            - "Salon resource config not found"
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
    public ResponseEntity<ApiResponseDTO<List<SlotResponseDTO>>> getAvailableSlots(
            @Parameter(description = "Request payload containing salonId, serviceIds, and date (yyyy-MM-dd)")
            @RequestBody AvailableSlotsRequest request) {
        List<SlotResponseDTO> slots = bookingService.getAvailableSlots(
                request.getSalonId(),
                request.getServiceIds(),
                request.getDate()
        );
        return ResponseEntity.ok(
                ApiResponseDTO.<List<SlotResponseDTO>>builder()
                        .status(true)
                        .message("Slots fetched successfully")
                        .data(slots)
                        .build()
        );
    }

    /**
     * Cancels an existing booking by its ID.
     * Updates the booking, associated services, and any pending payment to CANCELLED status.
     *
     * @param bookingId the unique identifier of the booking to cancel
     * @return ResponseEntity with cancellation confirmation
     */
    @PutMapping("/cancel/{bookingId}")
    @Operation(
            summary = "Cancel a booking",
            description = """
                    Cancels an existing booking and updates all associated services and pending
                    payments to CANCELLED status. Cannot cancel bookings in terminal or active states.
                    """,
            operationId = "cancelBooking"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking cancelled successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Booking not found"
                            - "Booking already cancelled"
                            - "Rejected booking cannot be cancelled"
                            - "Completed booking cannot be cancelled"
                            - "In Progress booking cannot be cancelled"
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
    public ResponseEntity<ApiResponseDTO<Void>> cancelBooking(
            @Parameter(description = "Unique identifier of the booking to cancel")
            @PathVariable UUID bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(
                ApiResponseDTO.<Void>builder()
                        .status(true)
                        .message("Booking cancelled successfully")
                        .build()
        );
    }

    /**
     * Returns a price breakdown for a given set of services and/or a package
     * without creating a booking.
     *
     * @param request the price summary request containing salonId, packageId, and serviceIds
     * @return BookingResponseDTO containing grossAmount, platformFee, taxAmount, discountAmount, and finalAmount
     */
    @PostMapping("/price-summary")
    @Operation(
            summary = "Get price summary for selected services",
            description = """
                    Calculates and returns a full price breakdown for the selected package
                    and/or add-on services without creating a booking. Useful for displaying
                    pricing details to the customer before checkout.
                    Breakdown includes: grossAmount, platformFee (₹15), taxAmount (18% GST on platform fee),
                    discountAmount (currently ₹0), and finalAmount.
                    """,
            operationId = "getPriceSummary"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Price summary fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Please select at least one service or package"
                            - "Selected package is not available for this salon"
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
    public ApiResponseDTO<BookingResponseDTO> getPriceSummary(
            @Parameter(description = "Request payload containing salonId, packageId (optional), and serviceIds (optional)")
            @RequestBody PriceSummaryRequestDTO request) {
        BookingResponseDTO response = bookingService.getPriceSummary(request);
        return ApiResponseDTO.<BookingResponseDTO>builder()
                .status(true)
                .message("Price summary fetched successfully")
                .data(response)
                .build();
    }
}