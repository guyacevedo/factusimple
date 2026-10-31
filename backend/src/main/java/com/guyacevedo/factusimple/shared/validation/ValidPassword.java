package com.guyacevedo.factusimple.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The one password policy of the application: register, activate account,
 * reset and change password all use it, so a user cannot set a weaker
 * password through one flow than through another. Change the rule here only.
 *
 * - at least 8 characters
 * - at least one uppercase letter, one lowercase letter and one digit
 * - any other character is allowed (ñ, accents, spaces, symbols)
 * - at most 72 bytes in UTF-8, the BCrypt limit (ñ or á count as 2)
 */
@Documented
@Constraint(validatedBy = PasswordMaxBytesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "La contraseña es requerida")
@Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
@Pattern(regexp = "^(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\d).+$",
        message = "La contraseña debe contener mayúsculas, minúsculas y números")
public @interface ValidPassword {

    String message() default "La contraseña es demasiado larga";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}