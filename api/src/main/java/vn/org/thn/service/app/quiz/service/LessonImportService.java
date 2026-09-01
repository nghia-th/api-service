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
import vn.org.thn.service.app.quiz.dto.LessonImportResponse;
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
 * Lesson bulk import from a fixed-column Excel/CSV template (added 2026-09-01, "phan bai hoc cho
 * phep import bang file") - same shape as task 4's {@link QuestionImportService} for Question,
 * reusing its error codes ({@code QUIZ_011 IMPORT_TOO_MANY_ROWS}, {@code QUIZ_012
 * IMPORT_FILE_UNREADABLE}) and its generic {@link ImportRowError}/{@link TemplateFile} DTOs
 * rather than duplicating them. Delegates the actual persistence of each valid row to {@link
 * LessonService#createFromImportRow} so both entry points (hand-entry and import) share one
 * persistence path, same reasoning as {@code QuestionImportService}.
 * <p>
 * The lesson's illustrative image is deliberately NOT part of the import file - same "text
 * fields via the file, the file/image itself uploaded afterwards, per row, once the entity has an
 * id" split already established for Question's audio and hand-entry Lesson's own image. A Parent
 * still attaches each imported lesson's image afterwards through {@code POST
 * /api/parent/lessons/{id}/image}.
 * <p>
 * LANGUAGE NOTE (same rule as {@code QuestionImportService}, read there for the full reasoning):
 * the template's column headers/example row and each {@link ImportRowError#getReason()} message
 * are Vietnamese on purpose - this is content the product displays to its end users (Vietnamese
 * parents), not developer-facing code prose.
 */
@Service
public class LessonImportService extends IBase {

    /** Same cap as {@code QuestionImportService#MAX_ROWS} - a Subject's lesson count per import is even less likely to approach this than a Lesson's question count, but kept identical for consistency rather than picking a different arbitrary number. */
    private static final int MAX_ROWS = 200;

    /** Same "mark the example row, skip it silently whether or not the parent deletes it" choice as {@code QuestionImportService#EXAMPLE_ROW_MARKER}. */
    private static final String EXAMPLE_ROW_MARKER = "[VÍ DỤ - XOÁ DÒNG NÀY TRƯỚC KHI IMPORT THẬT] ";

    private static final String[] HEADERS = {
            "Tên bài học", "Tóm tắt", "Nội dung", "Trang SGK"
    };

    private static final String[] EXAMPLE_ROW = {
            EXAMPLE_ROW_MARKER + "Unit 1 - Thì hiện tại đơn", "Ôn lại thì hiện tại đơn, cách dùng và dấu hiệu nhận biết.", "Nội dung chi tiết của bài học, có thể dài.", "12"
    };

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private LessonService lessonService;

    /** Same {@code ParsedRow} shape/reasoning as {@code QuestionImportService.ParsedRow} - see that class's javadoc for why {@code rowNumber} must not be conflated with the row's position in the returned list. */
    private record ParsedRow(int rowNumber, String[] cells) {
    }

    /** Builds the downloadable template file - "xlsx" (default) or "csv". */
    public TemplateFile generateTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return new TemplateFile(buildCsvTemplate(), "text/csv;charset=UTF-8", "lesson-import-template.csv");
        }
        return new TemplateFile(buildXlsxTemplate(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "lesson-import-template.xlsx");
    }

    /**
     * Best-effort row-by-row import into {@code subjectId} - one bad row does not stop the
     * others, same shape as {@code QuestionImportService#importFile}. {@code subjectId}
     * ownership is checked once, up front, before the file is even read.
     */
    public LessonImportResponse importFile(Long subjectId, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);

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
            String reason = importRow(subjectId, parentId, dataRows.get(i));
            if (reason == null) {
                successCount++;
            } else {
                errors.add(new ImportRowError(dataRowNumbers.get(i), reason));
            }
        }

        LessonImportResponse response = new LessonImportResponse();
        response.setTotalRows(dataRows.size());
        response.setSuccessCount(successCount);
        response.setErrors(errors);
        logInfo("Lesson import: subjectId={}, parentId={}, totalRows={}, successCount={}, errorCount={}",
                subjectId, parentId, dataRows.size(), successCount, errors.size());
        return response;
    }

    /**
     * Validates and, if valid, persists one data row. Returns null on success, or a Vietnamese
     * error message describing why the row was rejected. Only {@code name} (column A) is
     * required - {@code summary}/{@code content}/{@code textbookPage} are all optional, same as
     * hand-entry {@link vn.org.thn.service.app.quiz.dto.LessonCreateRequest}.
     */
    private String importRow(Long subjectId, Long parentId, String[] row) {
        String name = row[0].trim();
        String summary = row[1].trim();
        String content = row[2].trim();
        String textbookPageRaw = row[3].trim();

        if (name.isEmpty()) {
            return "Thiếu tên bài học (cột A)";
        }

        Integer textbookPage = null;
        if (!textbookPageRaw.isEmpty()) {
            try {
                textbookPage = Integer.parseInt(textbookPageRaw);
            } catch (NumberFormatException e) {
                return "Trang SGK (cột D) phải là số, nhận được: \"" + textbookPageRaw + "\"";
            }
            if (textbookPage <= 0) {
                return "Trang SGK (cột D) phải là số dương, nhận được: " + textbookPage;
            }
        }

        lessonService.createFromImportRow(subjectId, parentId, name,
                summary.isEmpty() ? null : summary, content.isEmpty() ? null : content, textbookPage);
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

    /** Reads every row of {@code file} (including the header) as fixed-width 4-column string arrays, dispatching on the original filename's extension. */
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
            log().warn("Failed to read lesson import file '{}': {}", filename, e.getMessage());
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "Could not parse file: " + e.getMessage());
        }
    }

    private List<ParsedRow> readXlsxRows(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[4];
                for (int col = 0; col < 4; col++) {
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
                String[] cells = new String[4];
                for (int col = 0; col < 4; col++) {
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
            Sheet sheet = workbook.createSheet("Lessons");
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
