package com.guyacevedo.factusimple.features.catalog.validation;

import com.guyacevedo.factusimple.features.catalog.Catalog;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value must be a code of the given DIAN catalog. null is valid: combine with @NotBlank when required. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CatalogCodeValidator.class)
public @interface ValidCatalogCode {

    Catalog value();

    /** Overridden by the validator with the catalog's name; kept because Bean Validation requires it. */
    String message() default "Código no válido. Consulta GET /v1/catalogs";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
