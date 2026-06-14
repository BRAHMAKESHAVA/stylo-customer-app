package org.backend.controller;

import com.razorpay.RazorpayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.RazorpayVerifyPaymentRequestDTO;
import org.backend.dto.response.RazorpayOrderResponseDTO;
import org.backend.enums.BookingStatus;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Booking;
import org.backend.repository.BookingRepository;
import org.backend.service.BookingService;
import org.backend.service.PartnerWebSocketService;
import org.backend.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing payments associated with salon bookings.
 * Handles Pay-at-Salon confirmation, Razorpay order creation, payment
 * verification, failure reporting, and refund initiation.
 * All endpoints operate under the base path "/api/payments".
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(
        name = "Payment Management",
        description = "Endpoints for managing booking payments including Pay-at-Salon, " +
                "Razorpay order creation, payment verification, failure handling, and refunds"
)
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepo;
    private final PartnerWebSocketService partnerWebSocketService;
    private final BookingService bookingService;

    /**
     * Confirms a Pay-at-Salon booking.
     * Creates a payment record with OFFLINE provider and advances the
     * booking to PENDING_PARTNER_CONFIRMATION status.
     *
     * @param bookingId the unique identifier of the booking
     * @return ApiResponseDTO confirming the action
     */
    @PostMapping("/{bookingId}/confirm-pay-at-salon")
    @Operation(
            summary = "Confirm Pay-at-Salon booking",
            description = """
                    Confirms the customer's intent to pay at the salon. Creates an OFFLINE
                    payment record and advances the booking to PENDING_PARTNER_CONFIRMATION.
                    Must be called within the payment hold window after booking creation.
                    The partner will then confirm or reject the booking before the customer pays.
                    """,
            operationId = "confirmPayAtSalon"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking confirmed. Pay at salon after partner approval."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Booking hold has expired. Please rebook."
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found"
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
    public ApiResponseDTO<Void> confirmPayAtSalon(
            @Parameter(description = "Unique identifier of the booking to confirm for Pay-at-Salon")
            @PathVariable UUID bookingId) {

        paymentService.confirmPayAtSalon(bookingId);

        return ApiResponseDTO.<Void>builder()
                .status(true)
                .message("Booking confirmed. Pay at salon after partner approval.")
                .data(null)
                .build();
    }

    /**
     * Creates a Razorpay order for online payment of a booking.
     * Reuses an existing INITIATED order if still valid.
     * Creates a new order if none exists or if the previous attempt failed.
     *
     * @param bookingId the unique identifier of the booking
     * @return ApiResponseDTO containing Razorpay order details (keyId, orderId, amount, currency)
     */
    @PostMapping("/order/{bookingId}")
    @Operation(
            summary = "Create a Razorpay order for a booking",
            description = """
                    Creates a Razorpay order to initiate online payment for a booking.
                    Behavior:
                    - If an INITIATED order already exists and no payment has been captured,
                      the existing order is reused (idempotent).
                    - If a previous order exists but payment was FAILED, Razorpay is checked
                      for a captured payment before creating a new order.
                    - Returns keyId, orderId, amount (in paise), and currency for use with
                      the Razorpay SDK on the client side.
                    Booking must be in PAYMENT_PENDING status.
                    """,
            operationId = "createOrder"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Razorpay order created or reused successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Booking has expired. Please select a slot again." (booking not in PAYMENT_PENDING)
                            - "Payment already completed for booking: {bookingId}"
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Not found. Possible reasons:
                            - "Booking not found: {bookingId}"
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
                    description = "Razorpay gateway error — order creation failed due to an upstream error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ApiResponseDTO<RazorpayOrderResponseDTO> createOrder(
            @Parameter(description = "Unique identifier of the booking for which to create a Razorpay order")
            @PathVariable UUID bookingId) {

        RazorpayOrderResponseDTO response = paymentService.createOrder(bookingId);

        return ApiResponseDTO.<RazorpayOrderResponseDTO>builder()
                .status(true)
                .message("Order created successfully")
                .data(response)
                .build();
    }

    /**
     * Verifies a completed Razorpay payment.
     * Validates signature, amount, and payment capture status.
     * On success, advances booking to PENDING_PARTNER_CONFIRMATION.
     *
     * @param bookingId the unique identifier of the booking
     * @param dto       the Razorpay verification payload (orderId, paymentId, signature)
     * @return confirmation message string
     * @throws RazorpayException if Razorpay API call fails
     */
    @PostMapping("/verify/{bookingId}")
    @Operation(
            summary = "Verify a Razorpay payment",
            description = """
                    Verifies the Razorpay payment after the customer completes checkout on the client side.
                    Performs the following checks in order:
                    1. Idempotency — skips if payment is already verified
                    2. Booking must be in PAYMENT_PENDING status
                    3. Provider must be RAZORPAY
                    4. Payment status must be 'captured' on Razorpay
                    5. HMAC-SHA256 signature validation
                    6. Amount match between booking and Razorpay
                    On success, booking advances to PENDING_PARTNER_CONFIRMATION.
                    On failure, booking is marked PAYMENT_FAILED.
                    """,
            operationId = "verifyPayment"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment verified and booking confirmed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Booking has expired. Please select a slot again." (booking not in PAYMENT_PENDING)
                            - "Invalid payment provider"
                            - "Payment does not belong to provided order"
                            - "Payment not captured"
                            - "Signature verification failed"
                            - "Amount mismatch"
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
                    description = "Razorpay gateway error — payment fetch failed due to an upstream error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public String verify(
            @Parameter(description = "Unique identifier of the booking to verify payment for")
            @PathVariable UUID bookingId,
            @Parameter(description = "Razorpay verification payload containing razorpayOrderId, razorpayPaymentId, and razorpaySignature")
            @RequestBody RazorpayVerifyPaymentRequestDTO dto) throws RazorpayException {
        paymentService.verifyPayment(bookingId, dto);
        return "Payment verified & Booking confirmed";
    }

    /**
     * Initiates a refund for a successfully paid booking.
     * Refund amount is time-based: full within 10 min, 50% within 60 min, none after.
     * Also cancels the booking after the refund is processed.
     *
     * @param bookingId the unique identifier of the booking
     * @param reason    the reason for refund
     * @return confirmation message string
     */
    @PostMapping("/refund/{bookingId}")
    @Operation(
            summary = "Initiate a refund for a booking",
            description = """
                    Initiates a Razorpay refund for a successfully paid booking and cancels the booking.
                    Refund amount is calculated based on time elapsed since payment:
                    - Within 10 minutes: 100% refund (FULL)
                    - Between 10 and 60 minutes: 50% refund (PARTIAL)
                    - After 60 minutes: No refund (NONE)
                    Payment must be in SUCCESS status and not already refunded.
                    """,
            operationId = "refundPayment"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refund initiated and booking cancelled successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Payment is not successful"
                            - "Refund already initiated"
                            - "Payment already refunded"
                            - "No refund applicable" (refund window has passed)
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
    public String refund(
            @Parameter(description = "Unique identifier of the booking to refund")
            @PathVariable UUID bookingId,
            @Parameter(description = "Reason for the refund (e.g., 'Customer requested cancellation')")
            @RequestParam String reason) {

        paymentService.refundPayment(bookingId, reason);

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setUpdatedDate(LocalDateTime.now());
        bookingRepo.save(booking);

        // Notify customer about refund and cancellation
        BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
        partnerWebSocketService.notifyCustomer(
                booking.getCustomerId(),
                bookingResponse
        );

        return "Refund Successful";
    }

    /**
     * Marks a payment and its associated booking as FAILED.
     * Called from the client when the Razorpay SDK reports a payment failure.
     * Skips silently if the payment is already SUCCESS or FAILED.
     *
     * @param bookingId the unique identifier of the booking
     * @return confirmation message string
     */
    @PostMapping("/failed/{bookingId}")
    @Operation(
            summary = "Mark a payment as failed",
            description = """
                    Records a payment failure for a booking. Intended to be called from the client
                    when the Razorpay SDK callback returns a failure event, ensuring the backend
                    stays in sync with the gateway state.
                    Idempotent — silently skips if payment is already in SUCCESS or FAILED status.
                    """,
            operationId = "markPaymentFailed"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment marked as failed successfully"
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
    public String markPaymentFailed(
            @Parameter(description = "Unique identifier of the booking whose payment should be marked failed")
            @PathVariable UUID bookingId) {
        paymentService.markPaymentFailed(bookingId);
        return "Payment marked failed";
    }
}