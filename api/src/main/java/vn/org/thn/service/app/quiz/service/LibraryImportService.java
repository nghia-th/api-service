package vn.org.thn.service.app.quiz.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.ImportRowError;
import vn.org.thn.service.app.quiz.dto.LibraryImportResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static vn.org.thn.service.app.quiz.exception.QuizErrorCode.IMPORT_FILE_UNREADABLE;
import static vn.org.thn.service.app.quiz.exception.QuizErrorCode.IMPORT_TOO_MANY_ROWS;

/**
 * Library document bulk import from a fixed-column Excel/CSV template (2026-09-05, item 1 of the
 * 11-item batch request: "cho phep import file [danh sach sach] sau do upload sach giao khoa
 * sau" - the Admin bulk-creates the METADATA rows from a spreadsheet, then attaches each row's
 * actual PDF afterward one at a time via {@code AdminLibraryApi#attachFile}, since a spreadsheet
 * cannot carry file bytes). Same structure/conventions as {@link QuestionImportService} (task 4's
 * precedent) - best-effort per row, one bad row does not stop the others - but with NO fixed FK
 * parameter (unlike {@code lessonId} there): every row here carries its own full
 * grade/subjectName/curriculum/volume, since a Library document has no single owning parent
 * record to check up front.
 * <p>
 * LANGUAGE NOTE: same exception as {@link QuestionImportService}'s class javadoc - the template's
 * column headers/example row and every {@link ImportRowError#getReason()} message are Vietnamese
 * on purpose (product-facing text for a Vietnamese-facing product), unlike this file's own
 * comments/Javadoc which stay English per the codebase's standing rule.
 */
@Service
public class LibraryImportService extends IBase {

    /** Same cap as {@link QuestionImportService#MAX_ROWS} - not a spec requirement, a defensive default, flagged for review. */
    private static final int MAX_ROWS = 200;

    /** Same "mark the example row so import skips it either way" convention as {@link QuestionImportService#EXAMPLE_ROW_MARKER}. */
    private static final String EXAMPLE_ROW_MARKER = "[VÍ DỤ - XOÁ DÒNG NÀY TRƯỚC KHI IMPORT THẬT] ";

    private static final String[] HEADERS = {"Lớp", "Môn học", "Bộ sách", "Tập", "Tiêu đề"};

    private static final String[] EXAMPLE_ROW = {
            EXAMPLE_ROW_MARKER + "4", "Toán", "Kết nối tri thức", "1", "Toán 4 - Tập 1 - Kết nối tri thức"
    };

    @Autowired
    private LibraryService libraryService;

    /** Same {@code (rowNumber, cells)} shape as {@code QuestionImportService.ParsedRow} - see that record's javadoc for why {@code rowNumber} must not be confused with the row's position in the parsed list. */
    private record ParsedRow(int rowNumber, String[] cells) {
    }

    /** Builds the downloadable template file - "xlsx" (default) or "csv". */
    public TemplateFile generateTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return new TemplateFile(buildCsvTemplate(), "text/csv;charset=UTF-8", "library-import-template.csv");
        }
        return new TemplateFile(buildXlsxTemplate(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "library-import-template.xlsx");
    }

    /**
     * Best-effort row-by-row import - one bad row does not stop the others. No fixed FK ownership
     * check up front (see this class's javadoc) - every row is validated/deduplicated
     * independently via {@link LibraryService#isValidTaxonomy}/{@link LibraryService#existsExact}.
     * A duplicate row (AskUserQuestion 2026-09-05: "bao loi dong do, bo qua") is reported as a
     * per-row error and skipped - the existing row is left untouched.
     */
    public LibraryImportResponse importFile(MultipartFile file) {
        List<ParsedRow> rows = readRows(file);
        List<String[]> dataRows = new ArrayList<>();
        List<Integer> dataRowNumbers = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            ParsedRow row = rows.get(i);
            if (isBlankRow(row.cells()) || isExampleRow(row.cells())) {
                continue;
            }
            dataRows.add(row.cells());
            dataRowNumbers.add(row.rowNumber());
        }

        if (dataRows.size() > MAX_ROWS) {
            throw new BusinessException(IMPORT_TOO_MANY_ROWS,
                    "File has " + dataRows.size() + " data rows, maximum allowed per import is " + MAX_ROWS);
        }

        int successCount = 0;
        List<ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < dataRows.size(); i++) {
            String reason = importRow(dataRows.get(i));
            if (reason == null) {
                successCount++;
            } else {
                errors.add(new ImportRowError(dataRowNumbers.get(i), reason));
            }
        }

        LibraryImportResponse response = new LibraryImportResponse();
        response.setTotalRows(dataRows.size());
        response.setSuccessCount(successCount);
        response.setErrors(errors);
        logInfo("Library import: totalRows={}, successCount={}, errorCount={}",
                dataRows.size(), successCount, errors.size());
        return response;
    }

    /**
     * Validates and, if valid, creates one metadata-only {@code LibraryDocument} row (no PDF
     * attached yet - see {@link LibraryService#createMetadataOnly}). Returns null on success, or
     * a Vietnamese error message describing why the row was rejected. Never throws for a
     * row-level problem, matching {@link QuestionImportService#importRow}'s convention.
     */
    private String importRow(String[] row) {
        String gradeRaw = row[0].trim();
        String subjectName = row[1].trim();
        String curriculum = row[2].trim();
        String volume = row[3].trim();
        String title = row[4].trim();

        if (subjectName.isEmpty()) {
            return "Thiếu tên môn học (cột B)";
        }
        if (curriculum.isEmpty()) {
            return "Thiếu tên bộ sách (cột C)";
        }

        int grade;
        try {
            grade = Integer.parseInt(gradeRaw);
        } catch (NumberFormatException e) {
            return "Lớp (cột A) phải là số từ 1 đến 12, nhận được: \"" + gradeRaw + "\"";
        }

        if (!libraryService.isValidTaxonomy(grade, curriculum)) {
            return "Lớp (cột A) phải từ 1-12 và Bộ sách (cột C) phải có trong danh sách bộ sách hiện có - giá trị nhận được không hợp lệ";
        }

        if (libraryService.existsExact(grade, subjectName, curriculum, volume)) {
            return "Đã tồn tại sách với Lớp/Môn học/Bộ sách/Tập này";
        }

        try {
            libraryService.createMetadataOnly(grade, subjectName, curriculum,
                    volume.isEmpty() ? null : volume, title.isEmpty() ? null : title);
        } catch (BusinessException e) {
            // Defensive only - isValidTaxonomy already checked above, so this should not happen
            // in practice short of a race (curriculum deleted between the check and the save).
            return e.getMessage() != null ? e.getMessage() : "Không thể tạo dòng này";
        }
        return null;
    }

    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (!cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isExampleRow(String[] row) {
        return row[0].startsWith(EXAMPLE_ROW_MARKER);
    }

    /** Reads every row of {@code file} (including the header) as fixed-width 5-column string arrays, dispatching on the original filename's extension - same approach as {@link QuestionImportService#readRows}. */
    private List<ParsedRow> readRows(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".csv")) {
                return readCsvRows(file.getInputStream());
            }
            if (filename.endsWith(".xlsx")) {
                return readXlsxRows(file.getInputStream());
            }
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "Unrecognized file extension - expected .xlsx or .csv");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log().warn("Failed to read library import file '{}': {}", filename, e.getMessage());
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "Could not parse file: " + e.getMessage());
        }
    }

    private List<ParsedRow> readXlsxRows(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[5];
                for (int col = 0; col < 5; col++) {
                    Cell cell = row.getCell(col);
                    cells[col] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                rows.add(new ParsedRow(row.getRowNum() + 1, cells));
            }
        }
        if (rows.isEmpty()) {
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "File has no rows");
        }
        return rows;
    }

    private List<ParsedRow> readCsvRows(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build();
        try (CSVParser parser = format.parse(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            for (org.apache.commons.csv.CSVRecord record : parser) {
                String[] cells = new String[5];
                for (int col = 0; col < 5; col++) {
                    cells[col] = col < record.size() ? record.get(col).trim() : "";
                }
                rows.add(new ParsedRow((int) record.getRecordNumber(), cells));
            }
        }
        if (rows.isEmpty()) {
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "File has no rows");
        }
        return rows;
    }

    private byte[] buildXlsxTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Library");
            writeXlsxRow(sheet, 0, HEADERS);
            writeXlsxRow(sheet, 1, EXAMPLE_ROW);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build xlsx template", e);
        }
    }

    private static void writeXlsxRow(Sheet sheet, int rowIndex, String[] values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private byte[] buildCsvTemplate() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {
            printer.printRecord((Object[]) HEADERS);
            printer.printRecord((Object[]) EXAMPLE_ROW);
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build csv template", e);
        }
    }
}
