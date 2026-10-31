package com.guyacevedo.factusimple.features.catalog.validation;

import com.guyacevedo.factusimple.features.catalog.Catalog;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogCodeValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    record Sample(@ValidCatalogCode(Catalog.IDENTIFICATION_TYPE) String idTypeCode) {}

    record Payment(@ValidCatalogCode(Catalog.PAYMENT_METHOD) String method,
                   @ValidCatalogCode(Catalog.WITHHOLDING_TAX) String withholding,
                   @ValidCatalogCode(Catalog.FISCAL_RESPONSIBILITY) String responsibility) {}

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void codeOfTheCatalogIsValid() {
        assertTrue(validator.validate(new Sample("13")).isEmpty());
    }

    @Test
    void nullIsLeftToNotBlank() {
        assertTrue(validator.validate(new Sample(null)).isEmpty());
    }

    @Test
    void unknownCodeFailsWithTheCatalogName() {
        Set<ConstraintViolation<Sample>> violations = validator.validate(new Sample("99"));

        assertEquals(1, violations.size());
        assertEquals("No es un código válido de tipos de identificación. Consulta GET /v1/catalogs",
                violations.iterator().next().getMessage());
    }

    @Test
    void userInputIsNotInterpolatedIntoTheMessage() {
        Set<ConstraintViolation<Sample>> violations = validator.validate(new Sample("${1+1}"));

        assertEquals(1, violations.size());
        assertTrue(!violations.iterator().next().getMessage().contains("2"));
    }

    @Test
    void listsFollowTheFactusCodes() {
        assertTrue(validator.validate(new Payment("71", "06", "O-15")).isEmpty());
        // ReteICA (07) is not a Factus withholding; 0-15 written with a zero is not the DIAN code.
        assertEquals(2, validator.validate(new Payment("10", "07", "0-15")).size());
    }
}
