package org.backend.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.backend.validation.validator.CouponValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CouponValidator.class) // connects annotation with validator
@Target(ElementType.TYPE) // class-level, needs access to multiple fields together
@Retention(RetentionPolicy.RUNTIME) // available at runtime
public @interface ValidCoupon {

    String message() default "Invalid coupon configuration"; // default fallback message

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
