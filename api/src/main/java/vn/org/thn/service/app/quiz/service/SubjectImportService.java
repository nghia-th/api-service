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
import vn.org.thn.service.app.quiz.dto.SubjectImportResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.app.quiz.security.CurrentUser;
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
 * Subject bulk import from a fixed-column Excel/CSV template (2026-09-05, item 2 of the 11-item
 * batch request: "Phu huynh bulk-tao Mon hoc qua import file" - clarified via AskUserQuestion to
 * be a feature independent of the textbook Library, see {@code LibraryImportService}'s javadoc
 * for that sibling feature). Same conventions as {@link QuestionImportService}/{@link
 * LibraryImportService} - best-effort per row, one bad row does not stop the others - with a
 * fixed {@code classroomId} parameter (like {@code QuestionImportService}'s {@code lessonId}):
 * every imported Subject is attached to ONE Classroom chosen up front, validated for ownership
 * once before the file is even read.
 * <p>
 * LANGUAGE NOTE: same exception as {@link QuestionImportService}'s class javadoc - the template's
 * column header/example row and every {@link ImportRowError#getReason()} message are Vietnamese
 * on purpose (product-facing text), unlike this file's own comments/Javadoc which stay English.
 */
@Service
public class SubjectImportService extends IBase {

    /** Same cap as the other import features - not a spec requirement, a defensive default. */
    private static final int MAX_ROWS = 200;

    /** Same "mark the example row so import skips it either way" convention as {@link QuestionImportService#EXAMPLE_ROW_MARKER}. */
    private static final String EXAMPLE_ROW_MARKER = "[VÍ DỤ - XOÁ DÒNG NÀY TRƯỚC KHI IMPORT THẬT] ";

    private static final String[] HEADERS = {"Tên môn học"};

    private static final String[] EXAMPLE_ROW = {EXAMPLE_ROW_MARKER + "Toán"};

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private SubjectService subjectService;

    private record ParsedRow(int rowNumber, String[] cells) {
    }

    /** Builds the downloadable template file - "xlsx" (default) or "csv". */
    public TemplateFile generateTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return new TemplateFile(buildCsvTemplate(), "text/csv;charset=UTF-8", "subject-import-template.csv");
        }
        return new TemplateFile(buildXlsxTemplate(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "subject-import-template.xlsx");
    }

    /**
     * Best-effort row-by-row import into {@code classroomId} - one bad row does not stop the
     * others. {@code classroomId} ownership is checked once, up front, before the file is even
     * read (same convention as {@code QuestionImportService#importFile}'s {@code lessonId}
     * check). A duplicate row (same name already existing in this classroom) is reported as a
     * per-row error and skipped, same "bao loi dong do, bo qua" convention as the Library import.
     */
    public SubjectImportResponse importFile(Long classroomId, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        classroomService.getOwnedOrThrow(classroomId, parentId);

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
            String reason = importRow(classroomId, parentId, dataRows.get(i));
            if (reason == null) {
                successCount++;
            } else {
                errors.add(new ImportRowError(dataRowNumbers.get(i), reason));
            }
        }

        SubjectImportResponse response = new SubjectImportResponse();
        response.setTotalRows(dataRows.size());
        response.setSuccessCount(successCount);
        response.setErrors(errors);
        logInfo("Subject import: classroomId={}, parentId={}, totalRows={}, successCount={}, errorCount={}",
                classroomId, parentId, dataRows.size(), successCount, errors.size());
        return response;
    }

    /**
     * Validates and, if valid, persists one data row. Returns null on success, or a Vietnamese
     * error message describing why the row was rejected. Never throws for a row-level problem -
     * only {@link #importFile}'s up-front checks (classroomId ownership, too-many-rows,
     * unreadable file) throw.
     */
    private String importRow(Long classroomId, Long parentId, String[] row) {
        String name = row[0].trim();
        if (name.isEmpty()) {
            return "Thiếu tên môn học (cột A)";
        }
        if (subjectService.existsByName(classroomId, name)) {
            return "Đã tồn tại môn học với tên này trong lớp học";
        }
        subjectService.createFromImportRow(classroomId, parentId, name);
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

    /** Reads every row of {@code file} (including the header) as fixed-width 1-column string arrays, dispatching on the original filename's extension - same approach as {@link QuestionImportService#readRows}. */
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
            log().warn("Failed to read subject import file '{}': {}", filename, e.getMessage());
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "Could not parse file: " + e.getMessage());
        }
    }

    private List<ParsedRow> readXlsxRows(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                String[] cells = {cell == null ? "" : formatter.formatCellValue(cell).trim()};
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
                String[] cells = {record.size() > 0 ? record.get(0).trim() : ""};
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
            Sheet sheet = workbook.createSheet("Subjects");
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
