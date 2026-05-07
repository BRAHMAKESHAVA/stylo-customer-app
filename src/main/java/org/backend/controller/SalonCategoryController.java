package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.ServiceCategoryDTO;
import org.backend.dto.UpdateServiceCategoryRequestDTO;
import org.backend.dto.common.ApiResponseDto;
import org.backend.dto.common.PageResponse;
import org.backend.model.ServiceCategory;
import org.backend.service.CategoryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing salon service categories.
 * This controller provides endpoints for creating, updating, deleting, and retrieving
 * service categories associated with salons. All endpoints operate under the base path "/api/service-categories".
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-categories")
@Tag(
        name = "Service Category Management",
        description = "APIs for creating, updating, deleting, and retrieving salon service categories"
)
public class SalonCategoryController {

    private final CategoryService categoryService;

    /**
     * Creates a new service category.
     *
     * @param category the service category details to be created
     * @return ResponseEntity containing the API response with the created category data
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create service category",
            description = "Creates a new service category for a salon. " +
                    "Validates salon existence and ensures category name uniqueness within the salon.",
            operationId = "createCategory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service category created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or category already exists"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Salon not found"
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported media type"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> createCategory(@Valid @RequestBody ServiceCategoryDTO category){

        return ResponseEntity.ok(
                ApiResponseDto.<ServiceCategoryDTO>builder()
                        .status(true)
                        .message("Service category created successfully.")
                        .data(categoryService.createCategory(category))
                        .build()
        );
    }

    /**
     * Updates an existing service category.
     *
     * @param categoryId the ID of the category to be updated
     * @param category the updated category details
     * @return ResponseEntity containing the API response with the updated category data
     */
    @PutMapping(
            value = "/{categoryId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update service category",
            description = "Updates an existing service category for a salon. " +
                    "Validates salon ID, category existence, and ensures category name uniqueness.",
            operationId = "updateCategory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service category updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request, empty category name, or duplicate category name"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category or salon not found"
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported media type"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody UpdateServiceCategoryRequestDTO category){

        return ResponseEntity.ok(
                ApiResponseDto.<ServiceCategoryDTO>builder()
                        .status(true)
                        .message("Service category updated successfully.")
                        .data(categoryService.updateCategory(categoryId, category))
                        .build()
        );
    }

    /**
     * Deletes a service category for a specific salon.
     *
     * @param salonId the ID of the salon
     * @param categoryId the ID of the category to be deleted
     * @return ResponseEntity containing the API response confirming the deletion
     */
    @DeleteMapping(
            value = "/{salonId}/category/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Delete service category",
            description = "Soft deletes a service category for a specific salon by setting isActive to false.",
            operationId = "deleteCategory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service category deleted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found for this salon"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> deleteCategory(@PathVariable Long salonId, @PathVariable Long categoryId){

        categoryService.deleteCategory(salonId,categoryId);

        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .status(true)
                        .message("Service category deleted successfully.")
                        .data(null)
                        .build()
        );
    }

    /**
     * Retrieves all service categories for a specific salon.
     *
     * @param salonId the ID of the salon whose categories are being retrieved
     * @return ResponseEntity containing the API response with the list of categories
     */
    @GetMapping(
            value = "/salon/{salonId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get categories by salon",
            description = "Fetches all service categories associated with a specific salon.",
            operationId = "getCategoryBySalonId"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service categories fetched successfully",
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
    public ResponseEntity<?> getCategoryBySalonId(@PathVariable Long salonId){

        List<ServiceCategory> categories = categoryService.getCategoriesBySalon(salonId);

        String message = categories.isEmpty()
                ? "No service categories found for this salon."
                : "Service categories fetched successfully for the salon.";

        return ResponseEntity.ok(
                ApiResponseDto.<List<ServiceCategory>>builder()
                        .status(true)
                        .message(message)
                        .data(categories)
                        .build()
        );
    }

    /**
     * Retrieves all service categories.
     *
     * @return ResponseEntity containing the API response with the list of all categories
     */
    @GetMapping(
            value = "/all",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all service categories",
            description = "Fetches all service categories with pagination support.",
            operationId = "getAllCategories"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Categories fetched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page number or page size"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponseDto<PageResponse<ServiceCategory>>> getAllCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.<PageResponse<ServiceCategory>>builder()
                        .status(true)
                        .message("Categories fetched successfully")
                        .data(categoryService.getAllCategories(page, size))
                        .build()
        );
    }

}