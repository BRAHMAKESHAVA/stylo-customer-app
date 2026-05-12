package org.backend.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.backend.validation.validator.PinCodeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PinCodeValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPinCode {

    String message() default "Invalid PIN code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}