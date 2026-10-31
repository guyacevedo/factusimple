package com.guyacevedo.factusimple.features.catalog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value must be a DANE municipality code of GET /v1/catalogs/municipalities. null is valid. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MunicipalityValidator.class)
public @interface ValidMunicipality {

    String message() default "No es un código de municipio DANE válido. Consulta GET /v1/catalogs/municipalities";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
