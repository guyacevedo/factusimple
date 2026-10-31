package com.guyacevedo.factusimple.shared.util;

import com.guyacevedo.factusimple.shared.exception.BadRequestException;

/**
 * DIAN verification digit (DV) of a NIT: modulo 11 with fixed prime weights
 * applied from the last digit of the NIT backwards.
 *
 * A wrong DV makes the DIAN reject the invoice, and that rejection only shows
 * up later, so it is checked when the NIT is entered.
 */
public final class NitVerificationDigit {

    private static final int[] WEIGHTS = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};

    private NitVerificationDigit() {
    }

    /** @throws IllegalArgumentException if the NIT is not 1 to 15 digits */
    public static int calculate(String nit) {
        if (nit == null || !nit.matches("\\d{1,15}")) {
            throw new IllegalArgumentException("El NIT debe tener entre 1 y 15 dígitos");
        }
        int sum = 0;
        for (int i = 0; i < nit.length(); i++) {
            int digit = nit.charAt(nit.length() - 1 - i) - '0';
            sum += digit * WEIGHTS[i];
        }
        int remainder = sum % 11;
        return remainder >= 2 ? 11 - remainder : remainder;
    }

    /**
     * The DV to store: the calculated one when none was given (as Factus does),
     * or the given one if it matches.
     *
     * @throws BadRequestException if the given DV does not match the NIT
     */
    public static String resolve(String nit, String dv) {
        String expected = String.valueOf(calculate(nit));
        if (dv == null || dv.isBlank()) {
            return expected;
        }
        if (!expected.equals(dv.trim())) {
            throw new BadRequestException(String.format(
                    "El dígito de verificación del NIT %s es %s, no %s", nit, expected, dv.trim()));
        }
        return expected;
    }
}
