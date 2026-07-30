package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.common.PageResponse;
import org.backend.dto.request.CouponRequestDTO;
import org.backend.dto.response.CouponResponseDTO;
import org.backend.dto.response.CouponPartnerSummaryDTO;
import org.backend.dto.response.CouponSummaryDTO;
import org.backend.enums.CouponStatus;
import org.backend.model.Coupon;
import org.backend.service.CouponEligibilityService;
import org.backend.service.CouponService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(
        name = "Coupon Management",
        description = "Endpoints for managing coupons, including creation, update, retrieval, listing, and deletion."
)
public class CouponController {

    private final CouponService couponService;
    private final CouponEligibilityService couponEligibilityService;

    // CREATE COUPON
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create a new coupon",
            description = "Creates a new coupon with discount configuration, eligibility rules, validity, and usage limits.",
            operationId = "createCoupon"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - Possible reasons: invalid coupon configuration, duplicate coupon code, or missing createdBy", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<CouponResponseDTO> createCoupon(
            @Parameter(description = "Request body containing coupon details")
            @Valid @RequestBody CouponRequestDTO request
    ) {
        CouponResponseDTO response = couponService.createCoupon(request);
        return ApiResponseDTO.<CouponResponseDTO>builder()
                .status(true)
                .message("Coupon created successfully")
                .data(response)
                .build();
    }

    // UPDATE COUPON
    @PutMapping(
            value = "/{couponId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update an existing coupon",
            description = "Updates the details of an existing coupon by its unique identifier.",
            operationId = "updateCoupon"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - Possible reasons: invalid coupon configuration, duplicate coupon code, or missing updatedBy", content = @Content),
            @ApiResponse(responseCode = "404", description = "Coupon not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<CouponResponseDTO> updateCoupon(
            @Parameter(description = "Unique identifier of the coupon")
            @PathVariable UUID couponId,

            @Parameter(description = "Request body containing updated coupon details")
            @Valid @RequestBody CouponRequestDTO request
    ) {
        CouponResponseDTO response = couponService.updateCoupon(couponId, request);
        return ApiResponseDTO.<CouponResponseDTO>builder()
                .status(true)
                .message("Coupon updated successfully")
                .data(response)
                .build();
    }

    // GET COUPON BY ID
    @GetMapping(
            value = "/{couponId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get coupon by ID",
            description = "Retrieves the details of a specific coupon by its unique identifier.",
            operationId = "getCouponById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Coupon not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<CouponResponseDTO> getCouponById(
            @Parameter(description = "Unique identifier of the coupon")
            @PathVariable UUID couponId
    ) {
        CouponResponseDTO response = couponService.getCouponById(couponId);
        return ApiResponseDTO.<CouponResponseDTO>builder()
                .status(true)
                .message("Coupon fetched successfully")
                .data(response)
                .build();
    }

    // GET COUPON BY CODE
    @GetMapping(
            value = "/code/{couponCode}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get coupon by code",
            description = "Retrieves the details of a specific coupon by its coupon code.",
            operationId = "getCouponByCode"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Coupon not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<CouponResponseDTO> getCouponByCode(
            @Parameter(description = "Coupon code")
            @PathVariable String couponCode
    ) {
        CouponResponseDTO response = couponService.getCouponByCode(couponCode);
        return ApiResponseDTO.<CouponResponseDTO>builder()
                .status(true)
                .message("Coupon fetched successfully")
                .data(response)
                .build();
    }

    // GET ALL COUPONS BASED ON THE STATUSES
    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all coupons",
            description = "Retrieves a paginated list of coupons, optionally filtered by status.",
            operationId = "getAllCoupons"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupons retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<PageResponse<CouponResponseDTO>> getAllCoupons(
            @Parameter(description = "Filter by coupon status")
            @RequestParam(required = false) CouponStatus status,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<CouponResponseDTO> response = couponService.getAllCoupons(status, page, size);
        return ApiResponseDTO.<PageResponse<CouponResponseDTO>>builder()
                .status(true)
                .message("Coupons fetched successfully")
                .data(response)
                .build();
    }

    // GET COUPONS CREATED BY A SPECIFIC PARTNER
    @GetMapping(
            value = "/partner/{partnerId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get coupons created by a partner",
            description = "Retrieves a paginated list of coupons that were created by the given partner, " +
                    "optionally filtered by status.",
            operationId = "getCouponsByPartnerId"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupons retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<PageResponse<CouponResponseDTO>> getCouponsByPartnerId(
            @Parameter(description = "Partner ID whose coupons should be fetched")
            @PathVariable Long partnerId,

            @Parameter(description = "Filter by coupon status")
            @RequestParam(required = false) CouponStatus status,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<CouponResponseDTO> response = couponService.getCouponsByPartnerId(partnerId, status, page, size);
        return ApiResponseDTO.<PageResponse<CouponResponseDTO>>builder()
                .status(true)
                .message("Coupons fetched successfully")
                .data(response)
                .build();
    }

    // GET COUPON SUMMARY BY PARTNER — total configured / currently valid / redeemed / cancelled / remaining
    @GetMapping(
            value = "/partner/{partnerId}/summary",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get coupon summary for a partner",
            description = "Given a partner ID, returns totals, currently-valid count, and the redeemed/" +
                    "cancelled/remaining breakdown, aggregated across every coupon that partner created.",
            operationId = "getCouponSummaryByPartnerId"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Partner coupon summary fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<CouponPartnerSummaryDTO> getCouponSummaryByPartnerId(
            @Parameter(description = "Partner ID to summarize coupons for")
            @PathVariable Long partnerId
    ) {
        CouponPartnerSummaryDTO response = couponService.getCouponSummaryByPartnerId(partnerId);
        return ApiResponseDTO.<CouponPartnerSummaryDTO>builder()
                .status(true)
                .message("Partner coupon summary fetched successfully")
                .data(response)
                .build();
    }

    // GET COUPON SUMMARY BY CODE — total generated / currently valid / redeemed / cancelled / remaining
    @GetMapping(
            value = "/code/{couponCode}/summary",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get coupon redemption summary by code",
            description = "Given a coupon code, returns how many codes exist in total (the whole batch, " +
                    "for GENERATED coupons), how many are currently valid, and the redeemed/cancelled/" +
                    "remaining breakdown.",
            operationId = "getCouponSummaryByCode"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon summary fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Coupon not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<CouponSummaryDTO> getCouponSummaryByCode(
            @Parameter(description = "Coupon code to summarize")
            @PathVariable String couponCode
    ) {
        CouponSummaryDTO response = couponService.getCouponSummaryByCode(couponCode);
        return ApiResponseDTO.<CouponSummaryDTO>builder()
                .status(true)
                .message("Coupon summary fetched successfully")
                .data(response)
                .build();
    }

    // GET ELIGIBLE COUPONS FOR A CUSTOMER (fetch-time checks only — see CouponEligibilityService)
    @GetMapping(
            value = "/customer/{customerId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get coupons eligible for a customer",
            description = "Retrieves all coupons the given customer can currently see, based on target audience, " +
                    "booking-count range, and usage limits. Bill amount, city, and payment-method restrictions " +
                    "are NOT evaluated here — those are re-checked at apply time against an actual booking.",
            operationId = "getEligibleCouponsForUser"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Eligible coupons fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<List<Coupon>> getEligibleCouponsForUser(
            @Parameter(description = "Customer ID to fetch eligible coupons for")
            @PathVariable Long customerId
    ) {
        List<Coupon> response = couponEligibilityService.getEligibleCouponsForUser(customerId);
        return ApiResponseDTO.<List<Coupon>>builder()
                .status(true)
                .message("Eligible coupons fetched successfully")
                .data(response)
                .build();
    }

    // DELETE COUPON (SOFT DELETE)
    @DeleteMapping(
            value = "/{couponId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Delete a coupon",
            description = "Performs a soft delete of a coupon by marking it as deleted.",
            operationId = "deleteCoupon"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Coupon not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication is required or the provided token is invalid", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource", content = @Content)
    })
    public ApiResponseDTO<String> deleteCoupon(
            @Parameter(description = "Unique identifier of the coupon")
            @PathVariable UUID couponId
    ) {
        couponService.deleteCoupon(couponId);
        return ApiResponseDTO.<String>builder()
                .status(true)
                .message("Coupon deleted successfully")
                .data("SUCCESS")
                .build();
    }
}