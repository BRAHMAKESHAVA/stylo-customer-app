package org.backend.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.backend.dto.request.CouponRequestDTO;
import org.backend.enums.CouponGenerationType;
import org.backend.enums.CreatedByType;
import org.backend.enums.DiscountType;
import org.backend.enums.TargetType;
import org.backend.validation.annotation.ValidCoupon;

import java.math.BigDecimal;

public class CouponValidator implements ConstraintValidator<ValidCoupon, CouponRequestDTO> {

    @Override
    public boolean isValid(CouponRequestDTO request, ConstraintValidatorContext context) {

        if (request == null) {
            return true; // let @NotNull on the object itself (if any) handle this
        }

        context.disableDefaultConstraintViolation(); // disable default message
        boolean valid = true;

        // Ownership: partnerId mandatory when createdByType = PARTNER
        if (request.getCreatedByType() == CreatedByType.PARTNER
                && request.getPartnerId() == null) {
            addViolation(context, "partnerId", "partnerId is mandatory when createdByType is PARTNER");
            valid = false;
        }

        // Coupon Target: eligibleCustomerIds mandatory (non-empty) when targetType = SELECTED_USERS
        if (request.getTargetType() == TargetType.SELECTED_USERS
                && (request.getEligibleCustomerIds() == null || request.getEligibleCustomerIds().isEmpty())) {
            addViolation(context, "eligibleCustomerIds",
                    "eligibleCustomerIds must not be empty when targetType is SELECTED_USERS");
            valid = false;
        }

        // Generated Coupons: generatedCouponCount mandatory (> 0) when generationType = GENERATED
        if (request.getGenerationType() == CouponGenerationType.GENERATED
                && (request.getGeneratedCouponCount() == null || request.getGeneratedCouponCount() <= 0)) {
            addViolation(context, "generatedCouponCount",
                    "generatedCouponCount is mandatory and must be greater than 0 when generationType is GENERATED");
            valid = false;
        }

        // Validity: endDate must be strictly after startDate
        if (request.getStartDate() != null && request.getEndDate() != null
                && !request.getEndDate().isAfter(request.getStartDate())) {
            addViolation(context, "endDate", "endDate must be after startDate");
            valid = false;
        }

        // Bill Amount Eligibility: maximumBillAmount must be >= minimumBillAmount
        if (request.getMinimumBillAmount() != null && request.getMaximumBillAmount() != null
                && request.getMaximumBillAmount().compareTo(request.getMinimumBillAmount()) < 0) {
            addViolation(context, "maximumBillAmount",
                    "maximumBillAmount must be greater than or equal to minimumBillAmount");
            valid = false;
        }

        // Booking Eligibility: maximumBookingCount must be >= minimumBookingCount
        if (request.getMinimumBookingCount() != null && request.getMaximumBookingCount() != null
                && request.getMaximumBookingCount() < request.getMinimumBookingCount()) {
            addViolation(context, "maximumBookingCount",
                    "maximumBookingCount must be greater than or equal to minimumBookingCount");
            valid = false;
        }

        // Discount Configuration: percentage discount cannot exceed 100
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            addViolation(context, "discountValue", "discountValue cannot exceed 100 for PERCENTAGE discountType");
            valid = false;
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
