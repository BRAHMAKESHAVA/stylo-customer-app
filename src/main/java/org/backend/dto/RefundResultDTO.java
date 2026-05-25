package org.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundResultDTO {

    private BigDecimal refundAmount;
    private String paymentStatus;
    private String providerRefundId;
}