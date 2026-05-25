package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.CreateSalonResourceRequest;
import org.backend.dto.request.UpdateSalonResourceRequest;
import org.backend.dto.response.SalonResourceResponse;
import org.backend.service.SalonResourceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing salon resources.
 * This controller provides endpoints for creating, updating, deleting, and retrieving
 * salon resources. All endpoints operate under the base path "/api/salon-resources".
 */
@RestController
@RequestMapping("/api/salon-resources")
@RequiredArgsConstructor
@Tag(
        name = "Salon Resource Management",
        description = "APIs for creating, updating, retrieving, and deleting salon resources"
)
public class SalonResourceController {

    private final SalonResourceService resourceService;

    /**
     * Creates a new salon resource.
     *
     * @param request the salon resource details to be created
     * @return ResponseEntity containing the API response with the created resource data
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create salon resource",
            description = "Creates a new salon resource for a salon. " +
                    "Validates salon existence and ensures only one resource exists per salon.",
            operationId = "createResource"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salon resource created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or resource already exists for this salon",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon not found",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported media type",
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
    public ResponseEntity<ApiResponseDTO<SalonResourceResponse>> createResource(@Valid @RequestBody CreateSalonResourceRequest request) {

        return ResponseEntity.ok(
                ApiResponseDTO.<SalonResourceResponse>builder()
                        .status(true)
                        .message("Salon resource created successfully.")
                        .data(resourceService.createResource(request))
                        .build()
        );
    }

    /**
     * Updates an existing salon resource by ID.
     *
     * @param id the ID of the resource to be updated
     * @param request the updated resource details
     * @return ResponseEntity containing the API response with the updated resource data
     */
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update salon resource",
            description = "Updates an existing salon resource by resource ID and salon ID. " +
                    "Validates resource existence and resource count.",
            operationId = "updateResource"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salon resource updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request, missing salon ID, or invalid resource count",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource or salon not found",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported media type",
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
    public ResponseEntity<ApiResponseDTO<SalonResourceResponse>> updateResource(@PathVariable Long id, @Valid @RequestBody UpdateSalonResourceRequest request) {

        return ResponseEntity.ok(
                ApiResponseDTO.<SalonResourceResponse>builder()
                        .status(true)
                        .message("Salon resource updated successfully.")
                        .data(resourceService.updateResource(id, request))
                        .build()
        );
    }

    /**
     * Retrieves a salon resource by salon ID.
     *
     * @param salonId the ID of the salon whose resource is being retrieved
     * @return ResponseEntity containing the API response with the resource data
     */
    @GetMapping(
            value = "/salon/{salonId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get salon resource by salon ID",
            description = "Fetches salon resource details associated with a specific salon.",
            operationId = "getResourceBySalonId"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salon resource fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon or resource not found",
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
    public ResponseEntity<ApiResponseDTO<SalonResourceResponse>> getResourceBySalonId(@PathVariable Long salonId) {
        SalonResourceResponse resource = resourceService.getResourceBySalonId(salonId);

        return ResponseEntity.ok(
                ApiResponseDTO.<SalonResourceResponse>builder()
                        .status(true)
                        .message("Salon resource fetched successfully.")
                        .data(resource)
                        .build()
        );
    }

    /**
     * Deletes a salon resource by salon ID and resource ID.
     *
     * @param salonId the ID of the salon
     * @param resourceId the ID of the resource to be deleted
     * @return ResponseEntity containing the API response confirming the deletion
     */
    @DeleteMapping(
            value = "/{salonId}/resources/{resourceId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Delete salon resource",
            description = "Deletes a salon resource using salon ID and resource ID.",
            operationId = "deleteResource"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salon resource deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found for the provided salon and resource ID",
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
    public ResponseEntity<ApiResponseDTO<Void>> deleteResource(
            @PathVariable Long salonId,
            @PathVariable Long resourceId) {

        resourceService.deleteResource(salonId, resourceId);

        return ResponseEntity.ok(
                ApiResponseDTO.<Void>builder()
                        .status(true)
                        .message("Salon resource deleted successfully.")
                        .data(null)
                        .build()
        );
    }
}
