package org.backend.controller;

import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.RazorpayVerifyPaymentRequestDTO;
import org.backend.dto.response.RazorpayOrderResponseDTO;
import org.backend.enums.BookingStatus;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Booking;
import org.backend.repository.BookingRepository;
import org.backend.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepo;

    /**
     * SELECT PAYMENT MODE
     * POST /api/payments/{bookingId}/select-mode
     * Body: { "paymentMode": "ONLINE" }  or  { "paymentMode": "PAY_AT_SALON" }
     *
     * Called from payment screen after booking is created.
     * - PAY_AT_SALON → booking moves to PENDING_PARTNER_CONFIRMATION immediately
     * - ONLINE       → booking stays PAYMENT_PENDING, then call /create-order next
     */
//    @PostMapping("/{bookingId}/select-mode")
//    public ApiResponseDTO<Void> selectPaymentMode(
//            @PathVariable Long bookingId,
//            @RequestBody Map<String, String> body
//    ) {
//        paymentService.selectPaymentMode(bookingId, body.get("paymentMode"));
//
//        return ApiResponseDTO.<Void>builder()
//                .status(true)
//                .message("Payment mode selected successfully")
//                .data(null)
//                .build();
//    }

    // Pay at Salon — Proceed with booking confirmation, partner approval, then pay at salon
    @PostMapping("/{bookingId}/confirm-pay-at-salon")
    public ApiResponseDTO<Void> confirmPayAtSalon(@PathVariable Long bookingId) {

        paymentService.confirmPayAtSalon(bookingId);

        return ApiResponseDTO.<Void>builder()
                .status(true)
                .message("Booking confirmed. Pay at salon after partner approval.")
                .data(null)
                .build();
    }

    // CREATE ORDER-Online
    @PostMapping("/order/{bookingId}")
    public ApiResponseDTO<RazorpayOrderResponseDTO> createOrder(@PathVariable Long bookingId) {

        RazorpayOrderResponseDTO response = paymentService.createOrder(bookingId);

        return ApiResponseDTO.<RazorpayOrderResponseDTO>builder()
                .status(true)
                .message("Order created successfully")
                .data(response)
                .build();
    }

    // VERIFY PAYMENT
    @PostMapping("/verify/{bookingId}")
    public String verify(@PathVariable Long bookingId, @RequestBody RazorpayVerifyPaymentRequestDTO dto) throws RazorpayException {
        paymentService.verifyPayment(bookingId, dto);
        return "Payment verified & Booking confirmed";
    }

    // REFUND PAYMENT
    @PostMapping("/refund/{bookingId}")
    public String refund(@PathVariable Long bookingId, @RequestParam String reason) {

        paymentService.refundPayment(bookingId, reason);

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setUpdatedDate(LocalDateTime.now());

        bookingRepo.save(booking);
        return "Refund Successful";
    }

    // Failure
    @PostMapping("/failed/{bookingId}")
    public String markPaymentFailed(@PathVariable Long bookingId) {
        paymentService.markPaymentFailed(bookingId);
        return "Payment marked failed";
    }
}