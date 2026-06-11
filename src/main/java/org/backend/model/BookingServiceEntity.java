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
    @Column(name = "id")
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "service_price", precision = 10, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "service_duration")
    private Integer serviceDuration;
}