package com.mtms.importer.infrastructure.parser;

import java.util.List;

/**
 * P0-SEC-04 / BR-SEC-02
 * Parsed payload containing extracted headers and rows.
 */
public record ParsedImportData(
        List<String> rawHeaders,
        List<String> normalizedHeaders,
        List<ParsedRow> rows
) {
}
