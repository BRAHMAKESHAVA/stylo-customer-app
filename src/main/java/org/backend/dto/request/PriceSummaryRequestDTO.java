package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Price Summary Request",
        description = "DTO used for calculating the price summary of a booking. Contains salon, package, and selected services."
)
public class PriceSummaryRequestDTO {

    @Schema(description = "Unique identifier of the salon", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salonId;

    @Schema(description = "Unique identifier of the package", example = "501")
    private Long packageId;

    @Schema(description = "List of service IDs selected for the booking", example = "[201, 202, 203]")
    private List<Long> serviceIds;
}
