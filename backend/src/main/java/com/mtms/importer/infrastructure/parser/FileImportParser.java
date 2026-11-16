package com.mtms.importer.infrastructure.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * P0-SEC-04 / BR-SEC-02
 * Multi-format parser for Excel (.xlsx, .xls) and CSV (.csv) import files.
 * Extracts headers, cleans cells, applies formula sanitization, and indexes 1-based row numbers.
 */
@Component
public class FileImportParser {

    private final DataFormatter dataFormatter = new DataFormatter();

    public ParsedImportData parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String filenameLower = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";

        try (InputStream is = file.getInputStream()) {
            if (filenameLower.endsWith(".xlsx") || filenameLower.endsWith(".xls")) {
                return parseExcel(is);
            } else if (filenameLower.endsWith(".csv") || isCsvContentType(file.getContentType())) {
                return parseCsv(is);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported file format: '" + originalFilename + "'. Only Excel (.xlsx, .xls) and CSV (.csv) files are supported."
                );
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    private boolean isCsvContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.contains("csv") || lower.contains("text/plain");
    }

    private ParsedImportData parseExcel(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Workbook does not contain any sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Spreadsheet contains no data or header row");
            }

            // Find header row (first non-empty row)
            Row headerRow = null;
            int headerRowIndex = 0;
            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row != null && !isExcelRowEmpty(row)) {
                    headerRow = row;
                    headerRowIndex = r;
                    break;
                }
            }

            if (headerRow == null) {
                throw new IllegalArgumentException("Spreadsheet does not contain a valid header row");
            }

            List<String> rawHeaders = new ArrayList<>();
            List<String> normalizedHeaders = new ArrayList<>();
            int maxCols = headerRow.getLastCellNum();

            for (int c = 0; c < maxCols; c++) {
                Cell cell = headerRow.getCell(c);
                String val = cell != null ? dataFormatter.formatCellValue(cell).trim() : "";
                rawHeaders.add(val);
                normalizedHeaders.add(normalizeHeader(val));
            }

            List<ParsedRow> rows = new ArrayList<>();
            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isExcelRowEmpty(row)) {
                    continue;
                }

                int rowNumber = r + 1; // 1-based index matching spreadsheet UI
                Map<String, String> rawData = new LinkedHashMap<>();
                Map<String, Object> normalizedData = new LinkedHashMap<>();

                for (int c = 0; c < rawHeaders.size(); c++) {
                    String rawHeader = rawHeaders.get(c);
                    String normHeader = normalizedHeaders.get(c);
                    Cell cell = row.getCell(c);
                    String rawVal = cell != null ? dataFormatter.formatCellValue(cell).trim() : "";

                    if (!rawHeader.isBlank()) {
                        rawData.put(rawHeader, rawVal);
                    }
                    if (!normHeader.isBlank()) {
                        String sanitized = FormulaSanitizer.sanitize(rawVal);
                        normalizedData.put(normHeader, sanitized);
                    }
                }

                rows.add(new ParsedRow(rowNumber, rawData, normalizedData));
            }

            return new ParsedImportData(rawHeaders, normalizedHeaders, rows);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse Excel workbook: " + e.getMessage(), e);
        }
    }

    private boolean isExcelRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ParsedImportData parseCsv(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Strip UTF-8 BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            List<String> rawHeaders = parseCsvLine(headerLine);
            if (rawHeaders.isEmpty() || rawHeaders.stream().allMatch(String::isBlank)) {
                throw new IllegalArgumentException("CSV does not contain a valid header row");
            }

            List<String> normalizedHeaders = rawHeaders.stream()
                    .map(this::normalizeHeader)
                    .toList();

            List<ParsedRow> rows = new ArrayList<>();
            String line;
            int lineNumber = 2; // Line 1 is header, first data line is 2

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }

                List<String> tokens = parseCsvLine(line);
                if (tokens.isEmpty() || tokens.stream().allMatch(String::isBlank)) {
                    lineNumber++;
                    continue;
                }

                Map<String, String> rawData = new LinkedHashMap<>();
                Map<String, Object> normalizedData = new LinkedHashMap<>();

                for (int c = 0; c < rawHeaders.size(); c++) {
                    String rawHeader = rawHeaders.get(c);
                    String normHeader = normalizedHeaders.get(c);
                    String rawVal = (c < tokens.size()) ? tokens.get(c).trim() : "";

                    if (!rawHeader.isBlank()) {
                        rawData.put(rawHeader, rawVal);
                    }
                    if (!normHeader.isBlank()) {
                        String sanitized = FormulaSanitizer.sanitize(rawVal);
                        normalizedData.put(normHeader, sanitized);
                    }
                }

                rows.add(new ParsedRow(lineNumber, rawData, normalizedData));
                lineNumber++;
            }

            return new ParsedImportData(rawHeaders, normalizedHeaders, rows);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    public String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String h = header.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "_");
        if ("qty".equals(h)) {
            return "quantity";
        }
        if ("cost".equals(h) || "price".equals(h)) {
            return "unit_cost";
        }
        return h;
    }
}
