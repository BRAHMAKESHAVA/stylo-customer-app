package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backend.dto.*;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.common.PageResponse;
import org.backend.service.SalonSearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for searching salons based on location and services.
 * This controller provides endpoints for finding nearby salons and searching salons
 * that offer specific services. All endpoints operate under the base path "/api/search".
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(
        name = "Salons Discovery and Search",
        description = "APIs for searching salons based on location, name, and services"
)
public class SalonSearchController {

    private final SalonSearchService salonSearchService;

    /**
     * Retrieves nearby salons based on geographical coordinates and distance.
     * Supports optional pagination for large result sets.
     *
     * @param latitude the latitude of the search location
     * @param longitude the longitude of the search location
     * @param distance the search radius distance
     * @param unit the unit of distance (e.g., "KM" for kilometers)
     * @param page the page number for pagination (optional)
     * @param size the page size for pagination (optional)
     * @return ResponseEntity containing the API response with nearby salons data
     */
    @GetMapping(
            value = "/salon/nearby",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get nearby salons",
            description = "Fetches nearby salons based on latitude, longitude, and distance with pagination support.",
            operationId = "getNearbySalons"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Nearby salons fetched successfully or no salons found near your location.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page number, page size, or invalid request parameters",
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
    public ResponseEntity<?> getNearbySalons(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double distance,
            @RequestParam(defaultValue = "KM") String unit,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size
    ) {

        // With pagination
        if (page != null && size != null) {
            PageResponse<SalonDetailsDTO> response = salonSearchService.findNearbySalonsWithPagination(
                            latitude, longitude, distance, unit, page, size);
            return ResponseEntity.ok(
                    ApiResponseDTO.<PageResponse<SalonDetailsDTO>>builder()
                            .status(true)
                            .message("Nearby salons fetched successfully with pagination.")
                            .data(response)
                            .build()
            );
        }

        // Without pagination
        List<SalonDetailsDTO> salons = salonSearchService.findNearbySalons(latitude, longitude, distance, unit);
        String message = salons.isEmpty() ? "No salons found near your location." : "Nearby salons fetched successfully.";
        return ResponseEntity.ok(
                ApiResponseDTO.<List<SalonDetailsDTO>>builder()
                        .status(true)
                        .message(message)
                        .data(salons)
                        .build()
        );
    }

    /**
     * Searches for salons that offer specific services within a geographical area.
     *
     * @param latitude the latitude of the search location
     * @param longitude the longitude of the search location
     * @param distance the search radius distance
     * @param unit the unit of distance (e.g., "KM" for kilometers)
     * @param request the request containing the list of service names to search for
     * @return ResponseEntity containing the API response with salons offering the selected services
     */
    @PostMapping(
            value = "/salon/search-by-services",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Search salons by selected services",
            description = "Searches nearby salons that provide all selected services within a geographical radius.",
            operationId = "searchSalonsByServices"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salons fetched successfully or no salons found for the selected service.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "At least one valid service must be selected or invalid request parameters",
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
    public ResponseEntity<ApiResponseDTO<List<SalonSearchWithSelectedServicesResponseDTO>>> searchSalonsByServices(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double distance,
            @RequestParam String unit,
            @RequestBody SalonSearchWithSelectedServicesRequest request
    ) {
System.out.println(request);
        List<SalonSearchWithSelectedServicesResponseDTO> result =
                salonSearchService.findSalonsWithSelectedServices(
                        latitude,
                        longitude,
                        distance,
                        unit,
                        request.getServiceNames()
                );

        if (result.isEmpty())
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "No salons found for the selected service.", List.of()));

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Salons fetched successfully", result)
        );
    }

    /**
     * Searches nearby salons using keyword suggestions.
     *
     * @param latitude the latitude of the search location
     * @param longitude the longitude of the search location
     * @param distance the search radius distance
     * @param keyword the search keyword
     * @param unit the unit of distance
     * @return ResponseEntity containing the API response with salon suggestions
     */
    @GetMapping(
            value = "/salons/nearby/suggestions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Search nearby salons suggestions",
            description = "Searches nearby salons using keyword suggestions within a geographical area.",
            operationId = "searchNearbySalons"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results fetched successfully",
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
    public ResponseEntity<?> searchNearbySalons(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double distance,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "KM") String unit
    ) {
        List<SalonDetailsDTO> salons =
                salonSearchService.searchNearbySalons(
                        latitude, longitude, distance, unit, keyword);

        return ResponseEntity.ok(
                ApiResponseDTO.<List<SalonDetailsDTO>>builder()
                        .status(true)
                        .message("Search results fetched successfully")
                        .data(salons)
                        .build()
        );
    }

    /**
     * Retrieves salons by salon name.
     *
     * @param salonName the salon name
     * @param latitude the latitude of the search location
     * @param longitude the longitude of the search location
     * @param unit the unit of distance
     * @return ResponseEntity containing the API response with salon details
     */
    @GetMapping(
            value = "/salons/by-name",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get salons by name",
            description = "Fetches salons by salon name along with distance and salon details.",
            operationId = "getSalonsByName"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salons fetched successfully",
                    content = @Content
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
    public ResponseEntity<?> getSalonsByName(
            @RequestParam String salonName,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "KM") String unit
    ) {

        List<SalonDetailsDTO> salons =
                salonSearchService.getSalonsByName(salonName, latitude, longitude, unit);

        return ResponseEntity.ok(
                ApiResponseDTO.<List<SalonDetailsDTO>>builder()
                        .status(true)
                        .message("Salons fetched successfully")
                        .data(salons)
                        .build()
        );
    }
}