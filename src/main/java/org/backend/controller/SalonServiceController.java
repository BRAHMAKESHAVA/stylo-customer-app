package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.CategoryGroupDTO;
import org.backend.dto.CategoryResponse;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.CreateSalonServiceRequest;
import org.backend.dto.request.UpdateSalonServiceRequest;
import org.backend.dto.response.SalonServiceResponse;
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
                    description = "Services retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponseDTO<List<CategoryResponse>>> getAllServices() {
        List<CategoryResponse> services = serviceManager.getAllServices();

        return ResponseEntity.ok(
                ApiResponseDTO.<List<CategoryResponse>>builder()
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
                    description = "Service created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid service data or validation error",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            )
    })
    public ResponseEntity<?> createService(@Valid @RequestBody CreateSalonServiceRequest request){
        return ResponseEntity.ok(
                ApiResponseDTO.<SalonServiceResponse>builder()
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
                    description = "Service updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid service data or validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            )
    })
    public ResponseEntity<?> updateService(
            @Parameter(description = "Unique identifier of the service to update")
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateSalonServiceRequest request){
        return ResponseEntity.ok(
                ApiResponseDTO.<SalonServiceResponse>builder()
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
                    description = "Services retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            )
    })
    public ResponseEntity<?> getServicesBySalon(@PathVariable Long salonId,
                                         @RequestParam(required = false) Integer pageNo,
                                         @RequestParam(required = false) Integer pageSize) {

        // With pagination
        if (pageNo != null && pageSize != null) {

            Page<SalonServiceResponse> page = serviceManager.getServicesBySalon(salonId, pageNo, pageSize);

            String message = page.isEmpty()
                    ? "No services found for this salon."
                    : "Services fetched successfully with pagination.";

            return ResponseEntity.ok(
                    ApiResponseDTO.<Page<SalonServiceResponse>>builder()
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
                ApiResponseDTO.<List<CategoryGroupDTO>>builder()
                        .status(true)
                        .message(message)
                        .data(services)
                        .build()
        );
    }

    // Get Services By Salon & Category
    /**
     * Retrieves services for a specific salon and category.
     *
     * @param salonId the ID of the salon
     * @param categoryId the ID of the category
     * @return ResponseEntity containing the list of services
     */
    @GetMapping(
            value = "/salon/{salonId}/categories/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get services by salon and category",
            description = "Retrieves all services available for a specific salon under a selected category.",
            operationId = "getServicesByCategoryAndSalon"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Services retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon or category not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            )
    })
    public ResponseEntity<?> getServicesByCategoryAnsSalon(
            @Parameter(description = "Unique identifier of the salon")
            @PathVariable Long salonId,

            @Parameter(description = "Unique identifier of the category")
            @PathVariable Long categoryId){

        List<SalonServiceResponse> services =
                serviceManager.getServicesByCategoryAndSalon(salonId, categoryId);

        String message = services.isEmpty()
                ? "No services found for this category in the salon."
                : "Services fetched successfully for the selected category.";

        return ResponseEntity.ok(
                ApiResponseDTO.<List<SalonServiceResponse>>builder()
                        .status(true)
                        .message(message)
                        .data(services)
                        .build()
        );
    }

    // Get Service By ID
    /**
     * Retrieves service details by service ID.
     *
     * @param serviceId the ID of the service
     * @return ResponseEntity containing service details
     */
    @GetMapping(
            value = "/{serviceId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get service by ID",
            description = "Fetches complete details of a specific salon service using its unique service ID.",
            operationId = "getServiceById"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service details retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - You do not have permission to access this resource",
                    content = @Content
            )
    })
    public ResponseEntity<?> getServiceById(
            @Parameter(description = "Unique identifier of the service")
            @PathVariable Long serviceId){

        SalonServiceResponse service = serviceManager.getServiceById(serviceId);

        return ResponseEntity.ok(
                ApiResponseDTO.<SalonServiceResponse>builder()
                        .status(true)
                        .message("Service details fetched successfully.")
                        .data(service)
                        .build()
        );
    }
}