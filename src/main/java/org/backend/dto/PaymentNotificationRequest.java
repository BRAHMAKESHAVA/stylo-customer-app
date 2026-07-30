package org.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentNotificationRequest {

    private Long userId;

    private Long paymentId;

    private String amount;

}