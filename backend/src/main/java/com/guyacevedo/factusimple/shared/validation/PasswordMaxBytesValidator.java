package com.guyacevedo.factusimple.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * BCrypt only accepts up to 72 bytes and Spring Security throws above that.
 * The limit is in bytes, not characters: 72 characters with ñ can be 108 bytes.
 */
public class PasswordMaxBytesValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int BCRYPT_MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}