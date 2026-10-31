package com.guyacevedo.factusimple.shared.util;

import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NitVerificationDigitTest {

    /** Public NITs with their published DV. */
    @ParameterizedTest
    @CsvSource({
            "800197268, 4",  // DIAN
            "899999068, 1",  // Ecopetrol
            "890903938, 8",  // Bancolombia
            "860034313, 7",  // Davivienda
    })
    void calculatesThePublishedDigit(String nit, int dv) {
        assertEquals(dv, NitVerificationDigit.calculate(nit));
    }

    @Test
    void rejectsNonNumericNit() {
        assertThrows(IllegalArgumentException.class, () -> NitVerificationDigit.calculate("90012A456"));
        assertThrows(IllegalArgumentException.class, () -> NitVerificationDigit.calculate(null));
    }

    @Test
    void resolveFillsAMissingDigit() {
        assertEquals("4", NitVerificationDigit.resolve("800197268", null));
        assertEquals("4", NitVerificationDigit.resolve("800197268", " "));
    }

    @Test
    void resolveKeepsTheRightDigitAndRejectsAWrongOne() {
        assertEquals("4", NitVerificationDigit.resolve("800197268", "4"));
        BadRequestException wrong = assertThrows(BadRequestException.class,
                () -> NitVerificationDigit.resolve("800197268", "5"));
        assertEquals("El dígito de verificación del NIT 800197268 es 4, no 5", wrong.getMessage());
    }
}
