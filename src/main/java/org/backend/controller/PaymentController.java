package org.backend.controller;

import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.backend.dto.RazorpayOrderResponseDTO;
import org.backend.dto.RazorpayVerifyPaymentRequestDTO;
import org.backend.enums.BookingStatus;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Booking;
import org.backend.repository.BookingRepository;
import org.backend.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepo;

    // CREATE ORDER
    @PostMapping("/order/{bookingId}")
    public RazorpayOrderResponseDTO createOrder(@PathVariable Long bookingId) {
        return paymentService.createOrder(bookingId);
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