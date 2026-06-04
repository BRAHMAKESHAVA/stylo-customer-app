package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Available Slots Request",
        description = "DTO used for fetching available booking slots for a salon on a given date, based on selected services."
)
public class AvailableSlotsRequest {

    @Schema(description = "Unique identifier of the salon", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salonId;

    @Schema(description = "List of service IDs for which slots are requested", example = "[201, 202, 203]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> serviceIds;

    @Schema(description = "Date for which available slots are requested", example = "2026-06-04", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;
}
