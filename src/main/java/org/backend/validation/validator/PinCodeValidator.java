package org.backend.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.backend.validation.annotation.ValidPinCode;

public class PinCodeValidator implements ConstraintValidator<ValidPinCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        // null check
        if (value == null || value.trim().isEmpty()) {

            context.buildConstraintViolationWithTemplate(
                    "Pin code is required"
            ).addConstraintViolation();

            return false;
        }

        // remove spaces
        value = value.trim();

        // length check
        if (value.length() != 6) {

            context.buildConstraintViolationWithTemplate(
                    "Pin code must be exactly 6 digits"
            ).addConstraintViolation();

            return false;
        }

        // numeric check
        if (!value.matches("\\d+")) {

            context.buildConstraintViolationWithTemplate(
                    "Pin code must contain only numbers"
            ).addConstraintViolation();

            return false;
        }

        // first digit cannot be 0
        if (value.startsWith("0")) {

            context.buildConstraintViolationWithTemplate(
                    "Pin code cannot start with 0"
            ).addConstraintViolation();

            return false;
        }

        return true;
    }
}