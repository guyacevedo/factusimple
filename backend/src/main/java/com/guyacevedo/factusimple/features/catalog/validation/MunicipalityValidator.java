package com.guyacevedo.factusimple.features.catalog.validation;

import com.guyacevedo.factusimple.features.catalog.MunicipalityCatalog;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/** Spring builds the validators, so the catalog is injected. */
@RequiredArgsConstructor
public class MunicipalityValidator implements ConstraintValidator<ValidMunicipality, String> {

    private final MunicipalityCatalog municipalityCatalog;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || municipalityCatalog.contains(value);
    }
}
