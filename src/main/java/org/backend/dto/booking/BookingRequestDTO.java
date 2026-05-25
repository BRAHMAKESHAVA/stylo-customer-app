package org.backend.dto.booking;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingRequestDTO {
    private Long customerId;
    private Long salonId;
    private Long packageId;
    private List<Long> serviceIds;
    private LocalDateTime startTime;
    //private LocalDateTime endTime;
    private String paymentMode; // ONLINE / PAY_AT_SALON

}