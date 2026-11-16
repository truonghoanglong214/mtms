package com.mtms.importer.infrastructure.parser;

import java.math.BigDecimal;

/**
 * P0-SEC-04 / BR-SEC-02
 * Utility to sanitize formulas and neutralize CSV/Excel formula injection (DDE/Formula attacks).
 */
public final class FormulaSanitizer {

    private FormulaSanitizer() {
    }

    /**
     * Sanitizes cell string values to prevent formula execution in spreadsheet software.
     * If a value begins with '=', '@', '\t', '\r', or non-numeric '+', '-', it is escaped with a leading quote.
     */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (value.startsWith("\t") || value.startsWith("\r")) {
            return "'" + value;
        }

        String trimmed = value.stripLeading();
        if (trimmed.isEmpty()) {
            return value;
        }

        char firstChar = trimmed.charAt(0);
        if (firstChar == '=' || firstChar == '@') {
            return "'" + value;
        }

        if (firstChar == '+' || firstChar == '-') {
            try {
                new BigDecimal(trimmed);
                return value;
            } catch (NumberFormatException e) {
                return "'" + value;
            }
        }

        return value;
    }

    /**
     * Checks whether a value constitutes a formula injection risk.
     */
    public static boolean isFormula(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.startsWith("\t") || value.startsWith("\r")) {
            return true;
        }
        String trimmed = value.stripLeading();
        if (trimmed.isEmpty()) {
            return false;
        }
        char firstChar = trimmed.charAt(0);
        if (firstChar == '=' || firstChar == '@') {
            return true;
        }
        if (firstChar == '+' || firstChar == '-') {
            try {
                new BigDecimal(trimmed);
                return false;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }
}
