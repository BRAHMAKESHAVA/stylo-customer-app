package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
                    description = "Nearby salons fetched successfully or no salons found near your location."
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
                    description = "Salons fetched successfully or no salons found for the selected service."
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
                    description = "Search results fetched successfully"
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
    public ResponseEntity<?> searchNearbySalonSuggestions(
            @RequestParam double latitude,
            @RequestParam double longitude,
            //@RequestParam double distance,
            @RequestParam String keyword
            //@RequestParam(defaultValue = "KM") String unit
    ) {
        List<SalonDetailsDTO> salons = salonSearchService.searchNearbySalonSuggestions(latitude, longitude, keyword);

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
                    description = "Salons fetched successfully"
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
    public ResponseEntity<?> getNearbySalonByName(
            @RequestParam String salonName,
            @RequestParam double latitude,
            @RequestParam double longitude
            //@RequestParam double distance,
            //@RequestParam(defaultValue = "KM") String unit
    ) {

        //List<SalonDetailsDTO> salons = salonSearchService.getNearbySalonByName(salonName, latitude, longitude, distance,unit);

        List<SalonDetailsDTO> salons = salonSearchService.getNearbySalonByName(salonName, latitude, longitude);

        return ResponseEntity.ok(
                ApiResponseDTO.<List<SalonDetailsDTO>>builder()
                        .status(true)
                        .message("Salons fetched successfully")
                        .data(salons)
                        .build()
        );
    }

    /**
     * Retrieves a list of popular salons near the given coordinates within the specified distance.
     * Popularity ranking is determined by the database query (e.g. booking count or rating).
     * Distance is calculated using a bounding-box approximation with Haversine filtering.
     * Each result includes salon details, working hours, address, and a front-view image if available.
     *
     * @param latitude  latitude of the user's current location (e.g. 12.9716)
     * @param longitude longitude of the user's current location (e.g. 77.5946)
     * @param distance  search radius in the specified unit (KM or M)
     * @param unit      distance unit — "KM" (default) or "M"; controls both input and response distances
     * @param size      maximum number of results to return; must be between 1 and 20, defaults to 20
     * @return ResponseEntity containing ApiResponseDTO with a list of SalonDetailsDTO,
     *         or an empty list if no salons are found within the given range
     */
    @GetMapping("/salons/popular")
    @Operation(
            summary = "Get popular salons near a location",
            description = """
                Retrieves a list of popular salons within a specified distance from the given
                coordinates.
                
                Unit behavior:
                - KM (default): distance parameter and response distances are in kilometres
                - M: distance parameter is treated as metres and response distances are in metres
                
                Results are capped at 20 salons maximum. Each result includes salon details,
                working hours, address, and a front-view image URL if available.
                """,
            operationId = "getPopularSalons"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Popular salons fetched successfully. Returns an empty list if none found within range."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        Bad request. Possible reasons:
                        - "Size must be between 1 and 20"
                        """,
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ResponseEntity<?> getPopularSalons(
            @Parameter(description = "Latitude of the user's current location (e.g. 12.9716)")
            @RequestParam double latitude,

            @Parameter(description = "Longitude of the user's current location (e.g. 77.5946)")
            @RequestParam double longitude,

            @Parameter(description = "Search radius in the specified unit. For KM: use values like 1.5, 5, 10. For M: use values like 500, 1000, 5000.")
            @RequestParam double distance,

            @Parameter(description = "Unit for the distance parameter and response distances. Accepted values: KM (default), M")
            @RequestParam(defaultValue = "KM") String unit,

            @Parameter(description = "Maximum number of salons to return. Must be between 1 and 20. Defaults to 20.")
            @RequestParam(defaultValue = "20") Integer size
    ) {
        List<SalonDetailsDTO> popularSalons =
                salonSearchService.getPopularSalons(
                        latitude, longitude,
                        distance, unit, size
                );

        return ResponseEntity.ok(
                ApiResponseDTO.<List<SalonDetailsDTO>>builder()
                        .status(true)
                        .message("Popular salons fetched successfully.")
                        .data(popularSalons)
                        .build()
        );
    }
}