package org.backend.controller;

import com.razorpay.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling Razorpay webhook events.
 * Receives and processes payment lifecycle notifications from Razorpay,
 * including payment capture and failure events.
 * Endpoint operates under "/api/payments/webhook/razorpay".
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(
        name = "Payment Webhooks",
        description = "Endpoints for receiving and processing Razorpay payment lifecycle webhook events"
)
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * Receives and processes Razorpay webhook events.
     * Performs HMAC-SHA256 signature verification before delegating to the service layer.
     * Handles the following event types:
     * - payment.captured → marks payment SUCCESS, advances booking to PENDING_PARTNER_CONFIRMATION
     * - payment.failed   → marks payment FAILED, advances booking to PAYMENT_FAILED
     * Unrecognized event types are logged and ignored.
     *
     * @param payload   the raw JSON webhook payload from Razorpay
     * @param signature the HMAC-SHA256 signature from the X-Razorpay-Signature header
     * @return HTTP 200 on success, HTTP 400 on signature failure or processing error
     */
    @PostMapping("/webhook/razorpay")
    @Operation(
            summary = "Handle Razorpay webhook",
            description = """
                    Receives payment lifecycle events from Razorpay via webhook.
                    Signature is verified using HMAC-SHA256 before any processing occurs.
                    
                    Handled event types:
                    - payment.captured: marks payment as SUCCESS and advances booking to
                      PENDING_PARTNER_CONFIRMATION
                    - payment.failed: marks payment as FAILED and booking as PAYMENT_FAILED
                    - all other events: logged and acknowledged without state changes
                    
                    This endpoint is intended to be called by Razorpay servers only.
                    The X-Razorpay-Signature header is mandatory and used for payload verification.
                    Idempotent — if payment is already marked SUCCESS, the event is safely skipped.
                    """,
            operationId = "handleRazorpayWebhook"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Webhook received and processed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - "Invalid signature" (X-Razorpay-Signature does not match payload)
                            - "Webhook failed" (unexpected error during processing)
                            - "Invalid webhook signature" (secondary check inside service layer)
                            - "Payment not found" (no payment record matching the order ID)
                            - "Booking not found" (booking linked to payment no longer exists)
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ResponseEntity<String> handleWebhook(
            @Parameter(description = "Raw JSON webhook payload sent by Razorpay")
            @RequestBody String payload,
            @Parameter(description = "HMAC-SHA256 signature from Razorpay for payload verification")
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        try {
            String secret = "mysecret123";
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, secret);

            if (!isValid) {
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            paymentService.handleWebhook(payload, signature);

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook failed");
        }
    }
}