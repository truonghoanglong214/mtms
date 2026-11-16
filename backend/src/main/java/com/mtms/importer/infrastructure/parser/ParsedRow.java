package com.mtms.importer.infrastructure.parser;

import java.util.Map;

/**
 * P0-SEC-04 / BR-SEC-02
 * A single parsed row extracted from an Excel or CSV file.
 */
public record ParsedRow(
        int rowNumber,
        Map<String, String> rawData,
        Map<String, Object> normalizedData
) {
}
