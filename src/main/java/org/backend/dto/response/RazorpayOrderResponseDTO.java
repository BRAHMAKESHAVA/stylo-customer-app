package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Razorpay Order Response",
        description = "DTO representing the response received after creating a Razorpay order."
)
public class RazorpayOrderResponseDTO {

    @Schema(description = "Public key ID used for Razorpay authentication", example = "rzp_test_1DP5mmOlF5G5ag")
    private String keyId;

    @Schema(description = "Unique identifier of the Razorpay order", example = "order_DBJOWzybf0sJbb")
    private String orderId;

    @Schema(description = "Order amount in smallest currency unit (e.g., paise for INR)", example = "50000")
    private Long amount;

    @Schema(description = "Currency code for the order", example = "INR")
    private String currency;
}
