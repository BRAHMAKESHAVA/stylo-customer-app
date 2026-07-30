package org.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.common.PageResponse;
import org.backend.dto.request.CouponRequestDTO;
import org.backend.dto.response.CouponResponseDTO;
import org.backend.dto.response.CouponPartnerSummaryDTO;
import org.backend.dto.response.CouponSummaryDTO;
import org.backend.enums.*;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Coupon;
import org.backend.repository.CouponRedemptionRepository;
import org.backend.repository.CouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    // CREATE COUPON
    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO request) {
        String couponCode = normalizeCouponCode(request.getCouponCode());
        if (couponRepository.existsByCouponCodeIgnoreCase(couponCode)) {
            throw new DuplicateResourceException(
                    "Coupon code '" + request.getCouponCode() + "' already exists");
        }

        Coupon coupon = Coupon.builder()
                .couponCode(couponCode)
                .couponName(request.getCouponName().trim())
                .description(request.getDescription())
                .createdByType(request.getCreatedByType())
                .partnerId(request.getPartnerId())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maximumDiscountAmount(request.getMaximumDiscountAmount())
                .minimumBillAmount(request.getMinimumBillAmount())
                .maximumBillAmount(request.getMaximumBillAmount())
                .minimumBookingCount(request.getMinimumBookingCount())
                .maximumBookingCount(request.getMaximumBookingCount())
                .targetType(request.getTargetType())
                .eligibleCustomerIds(request.getEligibleCustomerIds())
                .existingPartnerCustomerOnly(request.getExistingPartnerCustomerOnly())
                .eligibleCities(request.getEligibleCities())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .globalUsageLimit(request.getGlobalUsageLimit())
                .perUserUsageLimit(request.getPerUserUsageLimit())
                .perPartnerUsageLimit(request.getPerPartnerUsageLimit())
                .dailyUsageLimit(request.getDailyUsageLimit())
                .generationType(request.getGenerationType())
                .generatedCouponCount(request.getGeneratedCouponCount())
                .visibleToCustomer(request.getVisibleToCustomer())
                .status(request.getStatus())
                .deleted(Boolean.FALSE)
                .createdBy(request.getCreatedBy())
                .build();
        coupon.setEligiblePaymentMethodValues(request.getEligiblePaymentMethods());

        // Validate cross-field business rules
        validateBusinessRules(coupon);

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created | couponCode: {} | id: {}", saved.getCouponCode(), saved.getId());
        return toResponse(saved);
    }

    // UPDATE COUPON (PARTIAL UPDATE — only non-null fields in the request are applied)
    @Transactional
    public CouponResponseDTO updateCoupon(UUID id, CouponRequestDTO request) {
        String couponCode = normalizeCouponCode(request.getCouponCode());
        Coupon coupon = couponRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        // Basic Details
        if (couponCode != null && !couponCode.isEmpty()) {
            if (couponRepository.existsByCouponCodeIgnoreCaseAndIdNot(couponCode, id)) {
                throw new DuplicateResourceException("Coupon code '" + couponCode + "' already exists");
            }
            coupon.setCouponCode(couponCode);
        }

        if (request.getCouponName() != null && !request.getCouponName().trim().isEmpty()) {
            coupon.setCouponName(request.getCouponName().trim());
        }

        if (request.getDescription() != null) {
            coupon.setDescription(request.getDescription());
        }

        // Ownership
        if (request.getCreatedByType() != null) {
            coupon.setCreatedByType(request.getCreatedByType());
        }

        if (request.getPartnerId() != null) {
            coupon.setPartnerId(request.getPartnerId());
        }

        // Discount Configuration
        if (request.getDiscountType() != null) {
            coupon.setDiscountType(request.getDiscountType());
        }

        if (request.getDiscountValue() != null) {
            if (request.getDiscountValue().signum() <= 0) {
                throw new BadRequestException("discountValue must be greater than 0");
            }
            coupon.setDiscountValue(request.getDiscountValue());
        }

        if (request.getMaximumDiscountAmount() != null) {
            coupon.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        }

        // Bill Amount Eligibility
        if (request.getMinimumBillAmount() != null) {
            coupon.setMinimumBillAmount(request.getMinimumBillAmount());
        }

        if (request.getMaximumBillAmount() != null) {
            coupon.setMaximumBillAmount(request.getMaximumBillAmount());
        }

        // Booking Eligibility
        if (request.getMinimumBookingCount() != null) {
            coupon.setMinimumBookingCount(request.getMinimumBookingCount());
        }

        if (request.getMaximumBookingCount() != null) {
            coupon.setMaximumBookingCount(request.getMaximumBookingCount());
        }

        // Coupon Target
        if (request.getTargetType() != null) {
            coupon.setTargetType(request.getTargetType());
        }

        if (request.getEligibleCustomerIds() != null && !request.getEligibleCustomerIds().isEmpty()) {
            coupon.setEligibleCustomerIds(request.getEligibleCustomerIds());
        }

        if (request.getExistingPartnerCustomerOnly() != null) {
            coupon.setExistingPartnerCustomerOnly(request.getExistingPartnerCustomerOnly());
        }

        // Restrictions
        if (request.getEligibleCities() != null && !request.getEligibleCities().isEmpty()) {
            coupon.setEligibleCities(request.getEligibleCities());
        }

        if (request.getEligiblePaymentMethods() != null && !request.getEligiblePaymentMethods().isEmpty()) {
            coupon.setEligiblePaymentMethodValues(request.getEligiblePaymentMethods());
        }

        // Validity
        if (request.getStartDate() != null) {
            coupon.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            coupon.setEndDate(request.getEndDate());
        }

        // Usage Limits
        if (request.getGlobalUsageLimit() != null) {
            coupon.setGlobalUsageLimit(request.getGlobalUsageLimit());
        }

        if (request.getPerUserUsageLimit() != null) {
            coupon.setPerUserUsageLimit(request.getPerUserUsageLimit());
        }

        if (request.getPerPartnerUsageLimit() != null) {
            coupon.setPerPartnerUsageLimit(request.getPerPartnerUsageLimit());
        }

        if (request.getDailyUsageLimit() != null) {
            coupon.setDailyUsageLimit(request.getDailyUsageLimit());
        }

        // Generated Coupons
        if (request.getGenerationType() != null) {
            coupon.setGenerationType(request.getGenerationType());
        }

        if (request.getGeneratedCouponCount() != null) {
            coupon.setGeneratedCouponCount(request.getGeneratedCouponCount());
        }

        // Visibility
        if (request.getVisibleToCustomer() != null) {
            coupon.setVisibleToCustomer(request.getVisibleToCustomer());
        }

        // Status
        if (request.getStatus() != null) {
            coupon.setStatus(request.getStatus());
        }

        coupon.setUpdatedBy(request.getUpdatedBy());

        // Validate cross-field business rules after applying updates
        validateBusinessRules(coupon);

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon updated | couponCode: {} | id: {}", saved.getCouponCode(), saved.getId());
        return toResponse(saved);
    }

    // GET COUPON BY ID
    @Transactional(readOnly = true)
    public CouponResponseDTO getCouponById(UUID id) {
        Coupon coupon = couponRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        return toResponse(coupon);
    }

    // GET COUPON BY CODE
    @Transactional(readOnly = true)
    public CouponResponseDTO getCouponByCode(String couponCode) {
        String normalizeCouponCode = normalizeCouponCode(couponCode);
        Coupon coupon = couponRepository.findByCouponCodeIgnoreCaseAndDeletedFalse(normalizeCouponCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        return toResponse(coupon);
    }

    // GET ALL COUPONS (paginated, optionally filtered by status / partnerId)
    @Transactional(readOnly = true)
    public PageResponse<CouponResponseDTO> getAllCoupons(CouponStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Coupon> couponPage = couponRepository.search(status, pageable);

        return PageResponse.<CouponResponseDTO>builder()
                .page(couponPage.getNumber())
                .size(couponPage.getSize())
                .totalElements(couponPage.getTotalElements())
                .totalPages(couponPage.getTotalPages())
                .last(couponPage.isLast())
                .content(couponPage.getContent().stream().map(this::toResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponResponseDTO> getCouponsByPartnerId(
            Long partnerId, CouponStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Coupon> couponPage = couponRepository.findByPartnerId(partnerId, status, pageable);

        return PageResponse.<CouponResponseDTO>builder()
                .page(couponPage.getNumber())
                .size(couponPage.getSize())
                .totalElements(couponPage.getTotalElements())
                .totalPages(couponPage.getTotalPages())
                .last(couponPage.isLast())
                .content(couponPage.getContent().stream().map(this::toResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public CouponSummaryDTO getCouponSummaryByCode(String couponCode) {
        String normalizeCouponCode = normalizeCouponCode(couponCode);
        Coupon coupon = couponRepository.findByCouponCodeIgnoreCaseAndDeletedFalse(normalizeCouponCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        LocalDateTime now = LocalDateTime.now();

        long total = coupon.getGenerationType() == CouponGenerationType.GENERATED
                ? (coupon.getGeneratedCouponCount() != null ? coupon.getGeneratedCouponCount() : 0)
                : 1;

        long currentlyValid = isCurrentlyValid(coupon, now) ? 1 : 0;

        long redeemed = couponRedemptionRepository.countByCoupon_IdAndStatus(coupon.getId(), RedemptionStatus.REDEEMED);
        long cancelled = couponRedemptionRepository.countByCoupon_IdAndStatus(coupon.getId(), RedemptionStatus.CANCELLED);
        long remaining = Math.max(total - redeemed - cancelled, 0);

        return CouponSummaryDTO.builder()
                .couponCode(coupon.getCouponCode())
                .generationType(coupon.getGenerationType())
                .totalCoupons(total)
                .currentlyValidCoupons(currentlyValid)
                .redeemedCount(redeemed)
                .cancelledCount(cancelled)
                .remainingCount(remaining)
                .build();
    }

    // GET COUPON SUMMARY BY PARTNER — same per-row logic as getCouponSummaryByCode, summed
    // across every coupon record the partner has created.
    @Transactional(readOnly = true)
    public CouponPartnerSummaryDTO getCouponSummaryByPartnerId(Long partnerId) {

        List<Coupon> coupons = couponRepository.findAllByPartnerId(partnerId);
        LocalDateTime now = LocalDateTime.now();

        long totalCoupons = 0;
        long currentlyValid = 0;

        for (Coupon coupon : coupons) {
            totalCoupons++;
            if (isCurrentlyValid(coupon, now)) currentlyValid++;
        }

        List<UUID> couponIds = coupons.stream().map(Coupon::getId).toList();

        long redeemed = couponIds.isEmpty()
                ? 0
                : couponRedemptionRepository.countByCoupon_IdInAndStatus(couponIds, RedemptionStatus.REDEEMED);
        long cancelled = couponIds.isEmpty()
                ? 0
                : couponRedemptionRepository.countByCoupon_IdInAndStatus(couponIds, RedemptionStatus.CANCELLED);
        long remaining = Math.max(totalCoupons - redeemed - cancelled, 0);

        return CouponPartnerSummaryDTO.builder()
                .partnerId(partnerId)
                .totalCouponRecords(coupons.size())
                .totalCoupons(totalCoupons)
                .currentlyValidCoupons(currentlyValid)
                .redeemedCount(redeemed)
                .cancelledCount(cancelled)
                .remainingCount(remaining)
                .build();
    }

    // deleted is already guaranteed false here (findByCouponCodeIgnoreCaseAndDeletedFalse),
    // so this only needs to check status <> EXPIRED and the start/end date window.
    private boolean isCurrentlyValid(Coupon coupon, LocalDateTime now) {
        return coupon.getStatus() != CouponStatus.EXPIRED
                && !coupon.getStartDate().isAfter(now)
                && !coupon.getEndDate().isBefore(now);
    }

    // DELETE COUPON (SOFT DELETE)
    @Transactional
    public void deleteCoupon(UUID id) {
        Coupon coupon = couponRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setDeleted(Boolean.TRUE);
        coupon.setDeletedAt(LocalDateTime.now());
        couponRepository.save(coupon);
        log.info("Coupon soft-deleted | couponCode: {} | id: {}", coupon.getCouponCode(), coupon.getId());
    }

    // Cross-field checks applied to the merged entity after a partial update
    private void validateBusinessRules(Coupon coupon) {

        if (coupon.getCreatedByType() == CreatedByType.PARTNER
                && coupon.getPartnerId() == null) {
            throw new BadRequestException("partnerId is mandatory when created By PARTNER");
        }

        if (coupon.getTargetType() == TargetType.SELECTED_USERS
                && (coupon.getEligibleCustomerIds() == null || coupon.getEligibleCustomerIds().isEmpty())) {
            throw new BadRequestException("eligibleCustomerIds must not be empty when targetType is SELECTED_USERS");
        }

        if (coupon.getGenerationType() == CouponGenerationType.GENERATED
                && (coupon.getGeneratedCouponCount() == null || coupon.getGeneratedCouponCount() <= 0)) {
            throw new BadRequestException(
                    "generatedCouponCount is mandatory and must be greater than 0 when generationType is GENERATED");
        }

        if (coupon.getStartDate() != null && coupon.getEndDate() != null
                && !coupon.getEndDate().isAfter(coupon.getStartDate())) {
            throw new BadRequestException("endDate must be after startDate");
        }

        if (coupon.getMinimumBillAmount() != null && coupon.getMaximumBillAmount() != null
                && coupon.getMaximumBillAmount().compareTo(coupon.getMinimumBillAmount()) < 0) {
            throw new BadRequestException("maximumBillAmount must be greater than or equal to minimumBillAmount");
        }

        if (coupon.getMinimumBookingCount() != null && coupon.getMaximumBookingCount() != null
                && coupon.getMaximumBookingCount() < coupon.getMinimumBookingCount()) {
            throw new BadRequestException(
                    "maximumBookingCount must be greater than or equal to minimumBookingCount");
        }

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE
                && coupon.getDiscountValue() != null
                && coupon.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("discountValue cannot exceed 100 for PERCENTAGE discountType");
        }
    }

    // Normalize coupon code for case-insensitive comparisons and storage
    private String normalizeCouponCode(String couponCode) {
        return couponCode == null ? null : couponCode.trim().toUpperCase();
    }

    // MAPPER: Entity -> ResponseDTO
    private CouponResponseDTO toResponse(Coupon coupon) {
        return CouponResponseDTO.builder()
                .id(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .couponName(coupon.getCouponName())
                .description(coupon.getDescription())
                .createdByType(coupon.getCreatedByType())
                .partnerId(coupon.getPartnerId())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maximumDiscountAmount(coupon.getMaximumDiscountAmount())
                .minimumBillAmount(coupon.getMinimumBillAmount())
                .maximumBillAmount(coupon.getMaximumBillAmount())
                .minimumBookingCount(coupon.getMinimumBookingCount())
                .maximumBookingCount(coupon.getMaximumBookingCount())
                .targetType(coupon.getTargetType())
                .eligibleCustomerIds(coupon.getEligibleCustomerIds())
                .existingPartnerCustomerOnly(coupon.getExistingPartnerCustomerOnly())
                .eligibleCities(coupon.getEligibleCities())
                .eligiblePaymentMethods(coupon.getEligiblePaymentMethodValues())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .globalUsageLimit(coupon.getGlobalUsageLimit())
                .perUserUsageLimit(coupon.getPerUserUsageLimit())
                .perPartnerUsageLimit(coupon.getPerPartnerUsageLimit())
                .dailyUsageLimit(coupon.getDailyUsageLimit())
                .generationType(coupon.getGenerationType())
                .generatedCouponCount(coupon.getGeneratedCouponCount())
                .visibleToCustomer(coupon.getVisibleToCustomer())
                .status(coupon.getStatus())
                .deleted(coupon.getDeleted())
                .deletedAt(coupon.getDeletedAt())
                .createdBy(coupon.getCreatedBy())
                .createdAt(coupon.getCreatedAt())
                .updatedBy(coupon.getUpdatedBy())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}