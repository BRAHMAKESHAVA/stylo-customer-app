package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.backend.dto.PackageResponseDTO;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.CreatePackageRequestDTO;
import org.backend.dto.request.UpdatePackageRequestDTO;
import org.backend.dto.response.PackageRecommendationDTO;
import org.backend.service.PackageManagementService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(
        name = "Package Management",
        description = "Endpoints for managing salon packages, including creation, update, retrieval, and deletion."
)
public class PackageController {

    private final PackageManagementService packageManagementService;

    // CREATE PACKAGE
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new package",
            description = "Creates a new package for a salon with the provided details including name, description, price, and associated services.",
            operationId = "createPackage"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Package created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Possible reasons: " +
                            "duplicate package name, invalid services selected, " +
                            "selected services not belonging to the salon, or package price <= 0",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found - Possible reasons: " +
                            "salon not found or services not found",
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
    public ApiResponseDTO<PackageResponseDTO> createPackage(
            @Parameter(description = "Request body containing package details")
            @Valid @RequestBody CreatePackageRequestDTO request
    ) {
        PackageResponseDTO response = packageManagementService.createPackage(request);
        return ApiResponseDTO.<PackageResponseDTO>builder()
                .status(true)
                .message("Package created successfully")
                .data(response)
                .build();
    }

    // UPDATE PACKAGE
    @PutMapping(
            value = "/salon/{salonId}/package/{packageId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update an existing package",
            description = "Updates the details of an existing salon package. Only provided fields will be updated. " +
                    "Supports updating name, description, price, active status, and associated services.",
            operationId = "updatePackage",
            tags = { "Package Management" }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Package updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - Possible reasons: duplicate package name, invalid services selected, services not belonging to the salon, or package price <= 0", content = @Content),
            @ApiResponse(responseCode = "404", description = "Resource not found - Possible reasons: package not found for this salon", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<PackageResponseDTO> updatePackage(
            @Parameter(description = "Unique identifier of the salon")
            @Positive(message = "Salon ID must be greater than 0")
            @PathVariable Long salonId,

            @Parameter(description = "Unique identifier of the package")
            @Positive(message = "Package ID must be greater than 0")
            @PathVariable Long packageId,

            @Parameter(description = "Request body containing updated package details")
            @Valid @RequestBody UpdatePackageRequestDTO request
    ) {
        PackageResponseDTO response = packageManagementService.updatePackage(salonId, packageId, request);
        return ApiResponseDTO.<PackageResponseDTO>builder()
                .status(true)
                .message("Package updated successfully")
                .data(response)
                .build();
    }


    // GET PACKAGE BY ID
    @GetMapping(
            value = "/{packageId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get package by ID",
            description = "Retrieves the details of a specific salon package by its unique identifier. Only active packages are returned.",
            operationId = "getPackageById",
            tags = { "Package Management" }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Package retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Package not found or inactive",
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
    public ApiResponseDTO<PackageResponseDTO> getPackageById(
            @Parameter(description = "Unique identifier of the package")
            @Positive(message = "Package ID must be greater than 0")
            @PathVariable Long packageId
    ) {
        PackageResponseDTO response = packageManagementService.getPackageById(packageId);
        return ApiResponseDTO.<PackageResponseDTO>builder()
                .status(true)
                .message("Package fetched successfully")
                .data(response)
                .build();
    }

    // GET ALL PACKAGES
    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all active packages",
            description = "Retrieves all active salon packages available in the system.",
            operationId = "getAllPackages",
            tags = { "Package Management" }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Packages retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No active packages found",
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
    public ApiResponseDTO<List<PackageResponseDTO>> getAllPackages() {
        List<PackageResponseDTO> response = packageManagementService.getAllPackages();
        return ApiResponseDTO.<List<PackageResponseDTO>>builder()
                .status(true)
                .message("Packages fetched successfully")
                .data(response)
                .build();
    }

    // GET PACKAGES BY SALON ID
    @GetMapping(
            value = "/salon/{salonId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all active packages for a salon",
            description = "Retrieves all active packages associated with a specific salon by its unique identifier.",
            operationId = "getPackagesBySalonId",
            tags = { "Package Management" }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salon packages retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon not found or no active packages available",
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
    public ApiResponseDTO<List<PackageResponseDTO>> getPackagesBySalonId(
            @Parameter(description = "Unique identifier of the salon")
            @Positive(message = "Salon ID must be greater than 0")
            @PathVariable Long salonId
    ) {
        List<PackageResponseDTO> response = packageManagementService.getPackagesBySalonId(salonId);
        return ApiResponseDTO.<List<PackageResponseDTO>>builder()
                .status(true)
                .message("Salon packages fetched successfully")
                .data(response)
                .build();
    }

    // DELETE PACKAGE (SOFT DELETE)
    @DeleteMapping(
            value = "/salon/{salonId}/package/{packageId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Delete a package",
            description = "Performs a soft delete of a salon package by marking it inactive. The package must belong to the given salon.",
            operationId = "deletePackage",
            tags = { "Package Management" }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Package deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Package does not belong to the given salon",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Package not found",
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
    public ApiResponseDTO<String> deletePackage(
            @Parameter(description = "Unique identifier of the package")
            @Positive(message = "Package ID must be greater than 0")
            @PathVariable Long packageId,

            @Parameter(description = "Unique identifier of the salon")
            @Positive(message = "Salon ID must be greater than 0")
            @PathVariable Long salonId
    ) {
        packageManagementService.deletePackage(packageId, salonId);
        return ApiResponseDTO.<String>builder()
                .status(true)
                .message("Package deleted successfully")
                .data("SUCCESS")
                .build();
    }

    // GET RECOMMENDED PACKAGE
    @GetMapping("/recommendation")
    public ResponseEntity<ApiResponseDTO<PackageRecommendationDTO>> getRecommendation(
            @RequestParam Long salonId,
            @RequestParam Long packageId
    ) {
        PackageRecommendationDTO recommendation =
                packageManagementService.getRecommendedPackage(salonId, packageId);

        String message = (recommendation == null)
                ? "No package recommendation available"
                : "Package recommendation fetched successfully";

        ApiResponseDTO<PackageRecommendationDTO> response = ApiResponseDTO.<PackageRecommendationDTO>builder()
                .status(true)
                .message(message)
                .data(recommendation)
                .build();

        return ResponseEntity.ok(response);
    }
}
