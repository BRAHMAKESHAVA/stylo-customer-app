package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.*;
import org.backend.dto.common.ApiResponseDto;
import org.backend.model.SalonService;
import org.backend.service.SalonServices;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(
        name = "Salon Services Management",
        description = "Endpoints for managing salon services, categories, and service details"
)
public class SalonServiceController {

    private final SalonServices serviceManager;

    /**
     * Retrieves all available services across all salons.
     *
     * @return ResponseEntity containing a list of all services
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all services",
            description = "Retrieves a complete list of all services available in the system across all salons.",
            operationId = "getAllServices"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Services retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponseDto<List<CategoryResponse>>> getAllServices() {
        List<CategoryResponse> services = serviceManager.getAllServices();

        return ResponseEntity.ok(
                ApiResponseDto.<List<CategoryResponse>>builder()
                        .status(true)
                        .message("Services fetched successfully")
                        .data(services)
                        .build()
        );
    }

    /**
     * Creates a new service.
     *
     * @param request the service creation request
     * @return ResponseEntity containing the created service
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new service",
            description = "Creates a new service with the provided details.",
            operationId = "createService"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid service data or validation error"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> createService(@Valid @RequestBody SalonServiceDTO request){
        return ResponseEntity.ok(
                ApiResponseDto.<SalonServiceDTO>builder()
                        .status(true)
                        .message("Service created successfully.")
                        .data(serviceManager.createService(request))
                        .build()
        );
    }

    /**
     * Updates an existing service.
     *
     * @param serviceId the ID of the service to update
     * @param request the service update request
     * @return ResponseEntity containing the updated service
     */
    @PutMapping(
            value = "/{serviceId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update a service",
            description = "Updates an existing service with the provided details.",
            operationId = "updateService"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid service data or validation error"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> updateService(
            @Parameter(description = "Unique identifier of the service to update")
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request){
        return ResponseEntity.ok(
                ApiResponseDto.<SalonServiceDTO>builder()
                        .status(true)
                        .message("Service updated successfully.")
                        .data(serviceManager.updateService(serviceId, request))
                        .build()
        );
    }

    /**
     * Retrieves services for a specific salon with optional pagination.
     *
     * @param salonId the ID of the salon
     * @param pageNo the page number (optional)
     * @param pageSize the page size (optional)
     * @return ResponseEntity containing the services
     */
    @GetMapping(
            value = "/salon/{salonId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get services by salon",
            description = "Retrieves all services offered by a specific salon. Supports both paginated and non-paginated results.",
            operationId = "getServicesBySalon"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Services retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> getServicesBySalon(@PathVariable Long salonId,
                                         @RequestParam(required = false) Integer pageNo,
                                         @RequestParam(required = false) Integer pageSize) {

        // With pagination
        if (pageNo != null && pageSize != null) {

            Page<SalonService> page = serviceManager.getServicesBySalon(salonId, pageNo, pageSize);

            String message = page.isEmpty()
                    ? "No services found for this salon."
                    : "Services fetched successfully with pagination.";

            return ResponseEntity.ok(
                    ApiResponseDto.<Page<SalonService>>builder()
                            .status(true)
                            .message(message)
                            .data(page)
                            .build()
            );
        }

        // Without pagination
        List<CategoryGroupDTO> services = serviceManager.getServicesBySalon(salonId);

        String message = services.isEmpty()
                ? "No services found for this salon."
                : "Services fetched successfully.";

        return ResponseEntity.ok(
                ApiResponseDto.<List<CategoryGroupDTO>>builder()
                        .status(true)
                        .message(message)
                        .data(services)
                        .build()
        );
    }

    // Get Services By Salon & Category
    @GetMapping("/salon/{salonId}/categories/{categoryId}")
    public ResponseEntity<?> getServicesByCategoryAnsSalon(@PathVariable Long salonId,
                                                           @PathVariable Long categoryId){

        List<SalonService> services =
                serviceManager.getServicesByCategoryAndSalon(salonId, categoryId);

        String message = services.isEmpty()
                ? "No services found for this category in the salon."
                : "Services fetched successfully for the selected category.";

        return ResponseEntity.ok(
                ApiResponseDto.<List<SalonService>>builder()
                        .status(true)
                        .message(message)
                        .data(services)
                        .build()
        );
    }

    // Get Service By ID
    @GetMapping("/{serviceId}")
    public ResponseEntity<?> getServiceById(@PathVariable Long serviceId){

        SalonService service = serviceManager.getServiceById(serviceId);

        return ResponseEntity.ok(
                ApiResponseDto.<SalonService>builder()
                        .status(true)
                        .message("Service details fetched successfully.")
                        .data(service)
                        .build()
        );
    }
}