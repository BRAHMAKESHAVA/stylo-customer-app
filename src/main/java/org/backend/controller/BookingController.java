package org.backend.controller;

import lombok.RequiredArgsConstructor;
import org.backend.dto.booking.BookingRequestDTO;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.AvailableSlotsRequest;
import org.backend.service.BookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;


    // CREATE BOOKING
    @PostMapping
    public BookingResponseDTO createBooking(@RequestBody BookingRequestDTO bookingReq) {
        return bookingService.createBooking(bookingReq);
    }

    // GET USER BOOKINGS
    @GetMapping("/customer/{customerId}")
    public ApiResponseDTO<List<BookingResponseDTO>> getUserBookings(@PathVariable Long customerId) {

        List<BookingResponseDTO> bookings = bookingService.getCustomerBookings(customerId);

        return ApiResponseDTO.<List<BookingResponseDTO>>builder()
                .status(true)
                .message("Bookings fetched successfully")
                .data(bookings)
                .build();
    }

    @PostMapping("/available-slots")
    public List<String> getAvailableSlots(@RequestBody AvailableSlotsRequest request) {
        return bookingService.getAvailableSlots(
                request.getSalonId(),
                request.getServiceIds(),
                request.getDate()
        );
    }

    @PutMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return "Booking cancelled successfully";
    }
}