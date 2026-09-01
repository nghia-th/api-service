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
import vn.org.thn.service.app.quiz.dto.ChoiceRequest;
import vn.org.thn.service.app.quiz.dto.ImportRowError;
import vn.org.thn.service.app.quiz.dto.QuestionImportResponse;
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
 * Question/Choice bulk import from a fixed-column Excel/CSV template (task 4, added 2026-08-31 -
 * NOT OCR/free-form extraction, see {@code docs/dev/04-ngan-hang-cau-hoi.md}). Delegates the
 * actual persistence of each valid row to {@link QuestionService#createFromImportRow} so both
 * entry points (hand-entry and import) share one persistence path.
 * <p>
 * LANGUAGE NOTE (read before touching this file): unlike the rest of the codebase's code content
 * (comments/Javadoc/OpenAPI text - always English per the project's standing rule), the string
 * literals a Parent actually sees or fills in - the template's column headers/example row, and
 * each {@link ImportRowError#getReason()} message - are Vietnamese on purpose. Quiz-service is a
 * Vietnamese-facing product for Vietnamese parents/students, and the task 4 spec explicitly
 * requires the template columns and "reason" text to be Vietnamese (see the spec's exact column
 * names and "reason la cau mo ta loi ro rang bang tieng Viet"). This is content the product
 * displays to its end users, not developer-facing code prose, so it is treated differently from
 * the {@code @Schema}/{@code @Operation}/log-message English rule - flagged here explicitly since
 * it is a judgment call made without asking (see the task 4 completion note in the project's
 * Claude Projects doc for the full reasoning, for review).
 */
@Service
public class QuestionImportService extends IBase {

    /** Proposed cap, not a hard requirement from the spec - see task 4 doc. Flagged for review. */
    private static final int MAX_ROWS = 200;

    /**
     * Marks the template's illustrative example row so import silently skips it whether or not
     * the parent deletes it before uploading - chosen over relying on the parent to always delete
     * it (task 4 spec explicitly left this as a "pick one, document it" choice).
     */
    private static final String EXAMPLE_ROW_MARKER = "[VÍ DỤ - XOÁ DÒNG NÀY TRƯỚC KHI IMPORT THẬT] ";

    private static final String[] HEADERS = {
            "Câu hỏi", "Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3", "Lựa chọn 4", "Đáp án đúng", "Tag nhóm kiến thức"
    };

    private static final String[] EXAMPLE_ROW = {
            EXAMPLE_ROW_MARKER + "Con mèo có mấy chân?", "2", "4", "6", "", "2", "Động vật"
    };

    @Autowired
    private LessonService lessonService;

    @Autowired
    private QuestionService questionService;

    /**
     * One parsed data/header row plus the row number a Parent would actually see if they opened
     * the file themselves (Excel's own 1-based row numbers for xlsx; 1-based line numbers for
     * csv) - NOT the row's position in the returned list, which can diverge from it whenever a
     * physical row/line is absent from what the library hands back (POI's row iterator skips
     * rows that were never created in the sheet; Commons CSV skips blank lines by default). Using
     * the list position for {@link ImportRowError#getRowNumber()} was the original (buggy)
     * approach - it silently pointed a Parent at the wrong row whenever such a gap existed.
     */
    private record ParsedRow(int rowNumber, String[] cells) {
    }

    /** Builds the downloadable template file - "xlsx" (default) or "csv", see {@code format} in the task 4 spec. */
    public TemplateFile generateTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return new TemplateFile(buildCsvTemplate(), "text/csv;charset=UTF-8", "question-import-template.csv");
        }
        return new TemplateFile(buildXlsxTemplate(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "question-import-template.xlsx");
    }

    /**
     * Best-effort row-by-row import into {@code lessonId} - one bad row does not stop the others,
     * see {@code QuestionResponse}'s sibling {@link QuestionImportResponse}. {@code lessonId}
     * ownership is checked once, up front, before the file is even read (per the spec: an
     * unowned {@code lessonId} must fail immediately, without parsing).
     */
    public QuestionImportResponse importFile(Long lessonId, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        lessonService.getOwnedOrThrow(lessonId, parentId);

        List<ParsedRow> rows = readRows(file);
        // rows.get(0) is the header (row 1 as the Parent would see it in Excel); data rows are
        // everything after it. Each row's own ParsedRow#rowNumber is used for
        // ImportRowError#getRowNumber (not its position in this list - see ParsedRow's javadoc),
        // so a gap in the source file (a deleted Excel row, a blank CSV line) can't desync the
        // reported row number from the row the Parent actually needs to fix.
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
            String reason = importRow(lessonId, parentId, dataRows.get(i));
            if (reason == null) {
                successCount++;
            } else {
                errors.add(new ImportRowError(dataRowNumbers.get(i), reason));
            }
        }

        QuestionImportResponse response = new QuestionImportResponse();
        response.setTotalRows(dataRows.size());
        response.setSuccessCount(successCount);
        response.setErrors(errors);
        logInfo("Question import: lessonId={}, parentId={}, totalRows={}, successCount={}, errorCount={}",
                lessonId, parentId, dataRows.size(), successCount, errors.size());
        return response;
    }

    /**
     * Validates and, if valid, persists one data row. Returns null on success, or a Vietnamese
     * error message (see the class javadoc's LANGUAGE NOTE) describing why the row was rejected.
     * Never throws for a row-level problem - only {@link #importFile}'s up-front checks (lessonId
     * ownership, too-many-rows, unreadable file) throw.
     */
    private String importRow(Long lessonId, Long parentId, String[] row) {
        String content = row[0].trim();
        String choice1 = row[1].trim();
        String choice2 = row[2].trim();
        String choice3 = row[3].trim();
        String choice4 = row[4].trim();
        String answerRaw = row[5].trim();
        String knowledgeTag = row[6].trim();

        if (content.isEmpty()) {
            return "Thiếu nội dung câu hỏi (cột A)";
        }
        if (choice1.isEmpty() || choice2.isEmpty()) {
            return "Thiếu lựa chọn bắt buộc (cột B hoặc C)";
        }

        int answerIndex;
        try {
            answerIndex = Integer.parseInt(answerRaw);
        } catch (NumberFormatException e) {
            return "Đáp án đúng (cột F) phải là số từ 1 đến 4, nhận được: \"" + answerRaw + "\"";
        }
        if (answerIndex < 1 || answerIndex > 4) {
            return "Đáp án đúng (cột F) phải là số từ 1 đến 4, nhận được: " + answerIndex;
        }

        String[] choiceValues = {choice1, choice2, choice3, choice4};
        if (choiceValues[answerIndex - 1].isEmpty()) {
            return "Đáp án đúng (cột F) trỏ tới lựa chọn đang để trống (cột " + (char) ('A' + answerIndex) + ")";
        }

        List<ChoiceRequest> choices = new ArrayList<>();
        for (int i = 0; i < choiceValues.length; i++) {
            if (choiceValues[i].isEmpty()) {
                continue;
            }
            ChoiceRequest choiceRequest = new ChoiceRequest();
            choiceRequest.setContent(choiceValues[i]);
            choiceRequest.setCorrect(i == answerIndex - 1);
            choices.add(choiceRequest);
        }

        questionService.createFromImportRow(lessonId, parentId, content, knowledgeTag.isEmpty() ? null : knowledgeTag, choices);
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

    /** Reads every row of {@code file} (including the header) as fixed-width 7-column string arrays, dispatching on the original filename's extension. */
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
            // Any parse-level failure (corrupt file, wrong format despite a matching extension,
            // completely empty file, ...) is reported as a normal 400, not left to bubble up into
            // base's generic 500 handler - see the task 4 spec's "khong crash 500" requirement.
            log().warn("Failed to read question import file '{}': {}", filename, e.getMessage());
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "Could not parse file: " + e.getMessage());
        }
    }

    private List<ParsedRow> readXlsxRows(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[7];
                for (int col = 0; col < 7; col++) {
                    Cell cell = row.getCell(col);
                    cells[col] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                // getRowNum() is 0-based; +1 matches the row number Excel itself displays.
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
        // CSVFormat.DEFAULT silently skips blank lines (ignoreEmptyLines=true), which would desync
        // CSVRecord#getRecordNumber() from the line the Parent actually sees in their file the
        // moment a blank line appears before a data row - turn that off so every physical line
        // (blank or not) becomes one record and the two stay aligned; isBlankRow(...) in
        // importFile still filters the resulting blank records out same as before.
        CSVFormat format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build();
        try (CSVParser parser = format.parse(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            for (org.apache.commons.csv.CSVRecord record : parser) {
                String[] cells = new String[7];
                for (int col = 0; col < 7; col++) {
                    cells[col] = col < record.size() ? record.get(col).trim() : "";
                }
                // getRecordNumber() is 1-based and, with ignoreEmptyLines off, tracks the file's
                // own line numbers exactly - same convention as the +1 used for xlsx above.
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
            Sheet sheet = workbook.createSheet("Questions");
            writeXlsxRow(sheet, 0, HEADERS);
            writeXlsxRow(sheet, 1, EXAMPLE_ROW);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // In-memory workbook write - not expected to fail; wrapped so this compiles as
            // unchecked, matching the rest of this class's exception-handling style.
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
