package org.backend.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PartnerBookingStatusUpdateRequestDTO {

    @Schema(description = "Unique identifier of the salon", example = "987")
    private Long salonId;

    @Schema(description = "Status of the booking. Allowed values: CONFIRMED, REJECTED",
            example = "CONFIRMED")
    private String status;

    @Schema(description = "Reason for rejection. Mandatory when status is REJECTED",
            example = "Customer did not show up")
    private String reason;
}
