package com.guyacevedo.factusimple.features.catalog.validation;

import com.guyacevedo.factusimple.features.catalog.Catalog;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CatalogCodeValidator implements ConstraintValidator<ValidCatalogCode, String> {

    private Catalog catalog;

    @Override
    public void initialize(ValidCatalogCode annotation) {
        this.catalog = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || catalog.contains(value)) {
            return true;
        }
        // Only the catalog's fixed label goes into the template, never the user's value:
        // message templates are interpolated, so user input there could be evaluated.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "No es un código válido de " + catalog.label() + ". Consulta GET /v1/catalogs")
                .addConstraintViolation();
        return false;
    }
}
