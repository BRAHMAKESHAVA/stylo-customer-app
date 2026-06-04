package org.backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Booking Request",
        description = "DTO used for creating a new booking. Contains customer, salon, package, selected services, and start time."
)
public class BookingRequestDTO {

    @Schema(description = "Unique identifier of the customer", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "Unique identifier of the salon", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salonId;

    @Schema(description = "Unique identifier of the package", example = "501", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long packageId;

    @Schema(description = "List of service IDs included in the booking", example = "[201, 202, 203]")
    private List<Long> serviceIds;

    @Schema(description = "Booking start time", example = "2026-06-04T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    // If you later decide to include these fields, you can annotate them similarly:
    // @Schema(description = "Booking end time", example = "2026-06-04T11:30:00")
    // private LocalDateTime endTime;
    //
    // @Schema(description = "Payment mode", example = "ONLINE")
    // private String paymentMode; // ONLINE / PAY_AT_SALON
}
