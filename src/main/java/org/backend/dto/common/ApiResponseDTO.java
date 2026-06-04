package org.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "API Response",
        description = "Generic wrapper for all API responses. Contains status, message, and data payload."
)
public class ApiResponseDTO<T> {

    @Schema(description = "Indicates whether the request was successful", example = "true")
    private boolean status;

    @Schema(description = "Message describing the result of the request", example = "User registered successfully")
    private String message;

    @Schema(description = "Response data payload, varies depending on the endpoint")
    private T data;
}
