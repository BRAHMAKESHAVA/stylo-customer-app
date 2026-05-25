package org.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private Long serviceId;

    private String sourceType;
    private String status;

    private String serviceName;
    private BigDecimal servicePrice;
    private Integer serviceDuration;
}