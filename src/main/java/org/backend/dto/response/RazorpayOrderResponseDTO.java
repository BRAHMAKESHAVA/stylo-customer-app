package org.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayOrderResponseDTO {
    private String keyId;
    private String orderId;
    private Long  amount;
    private String currency;
}