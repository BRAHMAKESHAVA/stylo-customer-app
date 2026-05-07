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
import org.backend.dto.CreateAddressRequestDTO;
import org.backend.dto.UpdateAddressRequestDTO;
import org.backend.dto.common.AddressDTO;
import org.backend.dto.common.ApiResponseDto;
import org.backend.service.AddressService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing customer addresses.
 * This controller provides endpoints for creating, updating, deleting, and retrieving
 * customer addresses. All endpoints require a valid customer ID and operate under
 * the base path "/api/customer-address".
 */
@RestController
@RequestMapping("/api/customer-address")
@RequiredArgsConstructor
@Tag(
        name = "Customer Address Management",
        description = "Endpoints for managing customer addresses including CRUD operations"
)
public class CustomerAddressController {

    private final AddressService addressService;

    /**
     * Creates a new address for the specified customer.
     *
     * @param customerId the ID of the customer for whom the address is being created
     * @param address the address details to be created
     * @return ResponseEntity containing the API response with the created address data
     */
    @PostMapping(
            value = "/{customerId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new customer address",
            description = "Creates a new address entry for a specific customer. The address details are validated before creation.",
            operationId = "createAddress"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Address created successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid address data or validation error",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
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
    public ResponseEntity<ApiResponseDto<AddressDTO>> createAddress(
            @Parameter(description = "Unique identifier of the customer")
            @PathVariable Long customerId,
            @Valid @RequestBody AddressDTO address) {
        return ResponseEntity.ok()
                .body(ApiResponseDto.<AddressDTO>builder()
                        .status(true)
                        .message("Address created successfully")
                        .data(addressService.createAddress(customerId, address))
                        .build());
    }

    /**
     * Updates an existing address for the specified customer.
     *
     * @param customerId the ID of the customer whose address is being updated
     * @param addressId the ID of the address to be updated
     * @param addressDTO the updated address details
     * @return ResponseEntity containing the API response with the updated address data
     */
    @PutMapping(
            value = "/{customerId}/update/{addressId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update customer address",
            description = "Updates an existing address for a customer. Only provided fields will be updated.",
            operationId = "updateAddress"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Address updated successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid address data or validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer or address not found",
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
    public ResponseEntity<ApiResponseDto<AddressDTO>> updateAddress(
            @Parameter(description = "Unique identifier of the customer")
            @PathVariable Long customerId,
            @Parameter(description = "Unique identifier of the address to update")
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequestDTO addressDTO) {
        return ResponseEntity.ok(
                ApiResponseDto.<AddressDTO>builder()
                        .status(true)
                        .message("Address updated successfully")
                        .data(addressService.updateAddress(customerId, addressId, addressDTO))
                        .build()
        );
    }

    /**
     * Deletes an address for the specified customer.
     *
     * @param customerId the ID of the customer whose address is being deleted
     * @param addressId the ID of the address to be deleted
     * @return ResponseEntity containing the API response confirming the deletion
     */
    @DeleteMapping(
            value = "/{customerId}/delete/{addressId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Delete customer address",
            description = "Permanently deletes an address entry for a customer. This action cannot be undone.",
            operationId = "deleteAddress"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Address deleted successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer or address not found",
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
    public ResponseEntity<ApiResponseDto<String>> deleteAddress(
            @Parameter(description = "Unique identifier of the customer")
            @PathVariable Long customerId,
            @Parameter(description = "Unique identifier of the address to delete")
            @PathVariable Long addressId) {
        addressService.deleteAddress(customerId, addressId);
        return ResponseEntity.ok(ApiResponseDto.<String>builder()
                        .status(true)
                        .message("Address deleted successfully")
                        .data(null)
                        .build()
        );
    }

    /**
     * Retrieves a specific address by ID for the specified customer.
     *
     * @param customerId the ID of the customer whose address is being retrieved
     * @param addressId the ID of the address to be retrieved
     * @return ResponseEntity containing the API response with the address data
     */
    @GetMapping(
            value = "/{customerId}/address/{addressId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get customer address by ID",
            description = "Retrieves detailed information about a specific address for a customer.",
            operationId = "getAddressById"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Address retrieved successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer or address not found",
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
    public ResponseEntity<ApiResponseDto<AddressDTO>> getAddressById(@PathVariable Long customerId, @PathVariable Long addressId) {
        return ResponseEntity.ok(
                ApiResponseDto.<AddressDTO>builder()
                        .status(true)
                        .message("Address fetched successfully")
                        .data(addressService.getAddressById(customerId, addressId))
                        .build()
        );
    }

    /**
     * Retrieves all addresses for the specified customer.
     *
     * @param customerId the ID of the customer whose addresses are being retrieved
     * @return ResponseEntity containing the API response with the list of addresses
     */
    @GetMapping(
            value = "/{customerId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all addresses for a customer",
            description = "Retrieves all saved addresses for a specific customer.",
            operationId = "getAllAddresses"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Addresses retrieved successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
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
    public ResponseEntity<ApiResponseDto<List<AddressDTO>>> getAllAddresses(
            @Parameter(description = "Unique identifier of the customer")
            @PathVariable Long customerId) {
        List<AddressDTO> addresses = addressService.getAllAddresses(customerId);
        String message = addresses.isEmpty() ? "No addresses found" : "Addresses fetched successfully";
        return ResponseEntity.ok(
                ApiResponseDto.<List<AddressDTO>>builder()
                        .status(true)
                        .message(message)
                        .data(addresses)
                        .build()
        );
    }
}