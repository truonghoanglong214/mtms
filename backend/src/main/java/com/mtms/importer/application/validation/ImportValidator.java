package com.mtms.importer.application.validation;

import com.mtms.importer.domain.ImportKind;
import com.mtms.importer.domain.ImportRowStatus;
import com.mtms.importer.domain.RowValidationError;
import com.mtms.importer.infrastructure.parser.ParsedImportData;
import com.mtms.importer.infrastructure.parser.ParsedRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * P0-SEC-04 / BR-SEC-02 / TC-045
 * Business validator for staged import datasets.
 * Enforces schema requirements, duplicate detection across rows (TC-045), and field data integrity.
 */
@Component
public class ImportValidator {

    public static final String DUPLICATE_IDENTIFIER = "DUPLICATE_IDENTIFIER";
    public static final String MISSING_FIELD = "MISSING_FIELD";
    public static final String INVALID_QUANTITY = "INVALID_QUANTITY";
    public static final String INVALID_COST = "INVALID_COST";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";

    public List<ValidatedRow> validate(ImportKind kind, ParsedImportData parsedData) {
        validateRequiredHeaders(kind, parsedData.normalizedHeaders());

        String identifierField = getIdentifierField(kind);
        List<ParsedRow> rows = parsedData.rows();

        // 1. Group row numbers by identifier to detect duplicates within the batch (TC-045)
        Map<String, List<Integer>> identifierToRows = new LinkedHashMap<>();
        for (ParsedRow row : rows) {
            Object idObj = row.normalizedData().get(identifierField);
            if (idObj != null) {
                String idVal = idObj.toString().trim();
                if (!idVal.isEmpty()) {
                    identifierToRows.computeIfAbsent(idVal, k -> new ArrayList<>()).add(row.rowNumber());
                }
            }
        }

        // 2. Validate individual rows
        List<ValidatedRow> validatedRows = new ArrayList<>();
        for (ParsedRow row : rows) {
            List<RowValidationError> errors = new ArrayList<>();
            Map<String, Object> data = row.normalizedData();

            // Validate identifier presence and duplicate check
            Object idObj = data.get(identifierField);
            String idVal = idObj != null ? idObj.toString().trim() : "";

            if (idVal.isEmpty()) {
                errors.add(new RowValidationError(
                        identifierField,
                        MISSING_FIELD,
                        "Identifier '" + identifierField + "' cannot be blank"
                ));
            } else {
                List<Integer> duplicateRows = identifierToRows.getOrDefault(idVal, Collections.emptyList());
                if (duplicateRows.size() > 1) {
                    // TC-045: Report all row indices containing the duplicate identifier
                    String rowsStr = duplicateRows.stream().map(String::valueOf).collect(Collectors.joining(", "));
                    errors.add(new RowValidationError(
                            identifierField,
                            DUPLICATE_IDENTIFIER,
                            "Duplicate identifier '" + idVal + "' found in rows " + rowsStr
                    ));
                }
            }

            // Kind-specific field validations
            switch (kind) {
                case OPENING_INVENTORY -> validateOpeningInventory(data, errors);
                case ITEMS_CATALOG -> validateItemsCatalog(data, errors);
                case SUPPLIERS -> validateSuppliers(data, errors);
            }

            ImportRowStatus status = errors.isEmpty() ? ImportRowStatus.VALID : ImportRowStatus.ERROR;
            validatedRows.add(new ValidatedRow(
                    row.rowNumber(),
                    row.rawData(),
                    row.normalizedData(),
                    errors,
                    status
            ));
        }

        return validatedRows;
    }

    public void validateRequiredHeaders(ImportKind kind, List<String> normalizedHeaders) {
        Set<String> present = Set.copyOf(normalizedHeaders);
        List<String> required = getRequiredHeaders(kind);

        List<String> missing = required.stream()
                .filter(col -> !present.contains(col))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required column(s) for " + kind + ": " + String.join(", ", missing)
            );
        }
    }

    public String getIdentifierField(ImportKind kind) {
        return switch (kind) {
            case OPENING_INVENTORY, ITEMS_CATALOG -> "item_code";
            case SUPPLIERS -> "supplier_code";
        };
    }

    public List<String> getRequiredHeaders(ImportKind kind) {
        return switch (kind) {
            case OPENING_INVENTORY -> List.of("item_code", "quantity", "unit", "unit_cost");
            case ITEMS_CATALOG -> List.of("item_code", "item_name", "unit");
            case SUPPLIERS -> List.of("supplier_code", "supplier_name");
        };
    }

    private void validateOpeningInventory(Map<String, Object> data, List<RowValidationError> errors) {
        // Quantity validation
        Object qVal = data.get("quantity");
        if (qVal == null || qVal.toString().trim().isEmpty()) {
            errors.add(new RowValidationError("quantity", MISSING_FIELD, "Quantity is required"));
        } else {
            try {
                BigDecimal qty = new BigDecimal(qVal.toString().trim());
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(new RowValidationError("quantity", INVALID_QUANTITY, "Quantity must be a positive number (> 0)"));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowValidationError("quantity", INVALID_FORMAT, "Quantity must be a numeric value"));
            }
        }

        // Unit validation
        Object uVal = data.get("unit");
        if (uVal == null || uVal.toString().trim().isEmpty()) {
            errors.add(new RowValidationError("unit", MISSING_FIELD, "Unit cannot be blank"));
        }

        // Unit cost validation
        Object costVal = data.get("unit_cost");
        if (costVal == null || costVal.toString().trim().isEmpty()) {
            errors.add(new RowValidationError("unit_cost", MISSING_FIELD, "Unit cost is required"));
        } else {
            try {
                BigDecimal cost = new BigDecimal(costVal.toString().trim());
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(new RowValidationError("unit_cost", INVALID_COST, "Unit cost must be non-negative (>= 0)"));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowValidationError("unit_cost", INVALID_FORMAT, "Unit cost must be a numeric value"));
            }
        }
    }

    private void validateItemsCatalog(Map<String, Object> data, List<RowValidationError> errors) {
        Object nameVal = data.get("item_name");
        if (nameVal == null || nameVal.toString().trim().isEmpty()) {
            errors.add(new RowValidationError("item_name", MISSING_FIELD, "Item name cannot be blank"));
        }

        Object uVal = data.get("unit");
        if (uVal == null || uVal.toString().trim().isEmpty()) {
            errors.add(new RowValidationError("unit", MISSING_FIELD, "Unit cannot be blank"));
        }
    }

    private void validateSuppliers(Map<String, Object> data, List<RowValidationError> errors) {
        Object nameVal = data.get("supplier_name");
        if (nameVal == null || nameVal.toString().trim().isEmpty()) {
            errors.add(new RowValidationError("supplier_name", MISSING_FIELD, "Supplier name cannot be blank"));
        }
    }
}
