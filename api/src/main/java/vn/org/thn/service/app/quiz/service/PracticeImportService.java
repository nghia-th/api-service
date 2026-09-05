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
import vn.org.thn.service.app.quiz.dto.PracticeGenerateRequest;
import vn.org.thn.service.app.quiz.dto.PracticeImportResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BaseException;
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
 * Bulk import of "On tap kien thuc" (practice test) generation from a fixed-column Excel/CSV
 * template (2026-09-04, per the user's explicit request: "cho phep import file" for the Parent's
 * practice-test creation area) - creates MANY practice Tests in one file upload, one row = one
 * {@code POST /api/parent/tests/practice} call. Same shape as {@link LessonImportService}/{@code
 * QuestionImportService} (reused error codes {@code QUIZ_011 IMPORT_TOO_MANY_ROWS}/{@code QUIZ_012
 * IMPORT_FILE_UNREADABLE}, same {@link ImportRowError}/{@link TemplateFile} DTOs, same
 * best-effort-per-row shape) - <b>NOT</b> a mode where the Parent hand-picks specific questions
 * via the file (that alternative was explicitly rejected when this feature was scoped): each row
 * still gets a freshly-randomized question set exactly like the existing hand-triggered "Ôn tập"
 * button, by delegating straight to {@link TestService#generatePractice} - this class only
 * resolves each row's Student NAME into an id and builds the same {@link PracticeGenerateRequest}
 * that button already sends.
 * <p>
 * <b>Row shape - Student + question count ONLY (2026-09-05, revised)</b>: the Subject is now
 * chosen ONCE, up front, as a query param on the import call itself - {@code subjectId} - exactly
 * like {@link LessonImportService}/{@code QuestionImportService} already do, instead of every row
 * naming its own Subject by name (the original 2026-09-04 design). Per the user's explicit
 * clarification ("mỗi lần import một đề ôn theo môn" - "each import is one practice test PER
 * SUBJECT"): one import file always targets exactly one Subject, but can still span MANY
 * Students (every row is Student + optional question count only) - {@code subjectId} ownership is
 * checked ONCE before the file is even read (see {@link #importFile}), then every row only needs
 * to resolve its own Student and check that Student's Classroom matches {@code subjectId}'s
 * Classroom (mirrors {@link TestService#generatePractice}'s own "Subject must be in the Student's
 * Classroom" rule, so a row can never resolve a Student that call would reject anyway). A Parent
 * has no reason to know internal numeric ids, so each row references its Student by the STUDENT'S
 * LOGIN USERNAME (unique system-wide - see {@code AuthService#loginStudent}, which looks it up the
 * same way with no additional scoping).
 * <p>
 * LANGUAGE NOTE (same rule as {@code QuestionImportService}, read there for the full reasoning):
 * the template's column headers/example row and each {@link ImportRowError#getReason()} message
 * are Vietnamese on purpose - this is content the product displays to its end users (Vietnamese
 * parents), not developer-facing code prose.
 */
@Service
public class PracticeImportService extends IBase {

    /** Same cap as {@code LessonImportService#MAX_ROWS}/{@code QuestionImportService#MAX_ROWS} - kept identical for consistency rather than picking a different arbitrary number. */
    private static final int MAX_ROWS = 200;

    /** Same "mark the example row, skip it silently whether or not the parent deletes it" choice as {@code QuestionImportService#EXAMPLE_ROW_MARKER}. */
    private static final String EXAMPLE_ROW_MARKER = "[VÍ DỤ - XOÁ DÒNG NÀY TRƯỚC KHI IMPORT THẬT] ";

    /** 2 columns now (2026-09-05) - Subject dropped from the file, see this class's javadoc. */
    private static final String[] HEADERS = {
            "Tên đăng nhập học sinh", "Số câu hỏi (bỏ trống = mặc định)"
    };

    private static final String[] EXAMPLE_ROW = {
            EXAMPLE_ROW_MARKER + "hs_minhanh", "10"
    };

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private TestService testService;

    /** Same {@code ParsedRow} shape/reasoning as {@code QuestionImportService.ParsedRow} - see that class's javadoc for why {@code rowNumber} must not be conflated with the row's position in the returned list. */
    private record ParsedRow(int rowNumber, String[] cells) {
    }

    /** Builds the downloadable template file - "xlsx" (default) or "csv". Not Subject-specific - the same 2-column template works for importing into any Subject. */
    public TemplateFile generateTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return new TemplateFile(buildCsvTemplate(), "text/csv;charset=UTF-8", "practice-test-import-template.csv");
        }
        return new TemplateFile(buildXlsxTemplate(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "practice-test-import-template.xlsx");
    }

    /**
     * Best-effort row-by-row import for the CURRENT Parent ({@link CurrentUser#get()}), all rows
     * generating practice Tests from the SAME {@code subjectId} - one bad row does not stop the
     * others, same shape as {@code QuestionImportService#importFile}. {@code subjectId} ownership
     * is checked once, up front, before the file is even read (same convention as {@code
     * LessonImportService#importFile}).
     */
    public PracticeImportResponse importFile(Long subjectId, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        Subject subject = subjectService.getOwnedOrThrow(subjectId, parentId);

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
            String reason = importRow(parentId, subject, dataRows.get(i));
            if (reason == null) {
                successCount++;
            } else {
                errors.add(new ImportRowError(dataRowNumbers.get(i), reason));
            }
        }

        PracticeImportResponse response = new PracticeImportResponse();
        response.setTotalRows(dataRows.size());
        response.setSuccessCount(successCount);
        response.setErrors(errors);
        logInfo("Practice test import: parentId={}, subjectId={}, totalRows={}, successCount={}, errorCount={}",
                parentId, subjectId, dataRows.size(), successCount, errors.size());
        return response;
    }

    /**
     * Validates, resolves and, if valid, generates one practice Test for one data row against the
     * already-resolved {@code subject}. Returns null on success, or a Vietnamese error message
     * describing why the row was rejected - including {@link BusinessException}s from {@link
     * TestService#generatePractice} itself (e.g. the resolved Subject having no questions at all
     * yet), which are caught here and turned into a per-row error exactly like a validation
     * failure, rather than aborting the whole file.
     */
    private String importRow(Long parentId, Subject subject, String[] row) {
        String username = row[0].trim();
        String questionCountRaw = row[1].trim();

        if (username.isEmpty()) {
            return "Thiếu tên đăng nhập học sinh (cột A)";
        }

        // Username is the Student's unique login handle system-wide (see
        // AuthService#loginStudent, which resolves it the same unscoped way) - resolved first,
        // THEN ownership-checked against the current parent, same "don't leak whether it exists
        // under someone else" caution as everywhere else ownership is checked in this codebase.
        Student student = studentRepository.query().eq(Student::getUsername, username).one();
        if (student == null || !student.getParentId().equals(parentId)) {
            return "Không tìm thấy học sinh với tên đăng nhập \"" + username + "\" thuộc tài khoản của anh";
        }

        // subjectId is fixed for the whole file (2026-09-05) - every row's Student must be in
        // THAT Subject's own Classroom, matching TestService#generatePractice's own "Subject must
        // be in the Student's Classroom" rule, so a row can never resolve a Student that call
        // would reject anyway.
        if (!subject.getClassroomId().equals(student.getClassroomId())) {
            return "Học sinh \"" + username + "\" không thuộc lớp có môn học này";
        }

        Integer questionCount = null;
        if (!questionCountRaw.isEmpty()) {
            try {
                questionCount = Integer.parseInt(questionCountRaw);
            } catch (NumberFormatException e) {
                return "Số câu hỏi (cột B) phải là số, nhận được: \"" + questionCountRaw + "\"";
            }
            if (questionCount <= 0) {
                return "Số câu hỏi (cột B) phải là số dương, nhận được: " + questionCount;
            }
        }

        PracticeGenerateRequest request = new PracticeGenerateRequest();
        request.setStudentId(student.getId());
        request.setSubjectId(subject.getId());
        request.setQuestionCount(questionCount);
        // name left null on purpose - TestService defaults it to "Ôn tập <tên môn>".

        try {
            testService.generatePractice(request);
        } catch (BaseException e) {
            return e.getMessage() != null ? e.getMessage() : e.getErrorCode().getMessage();
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

    /** Reads every row of {@code file} (including the header) as fixed-width 2-column string arrays, dispatching on the original filename's extension. */
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
            log().warn("Failed to read practice import file '{}': {}", filename, e.getMessage());
            throw new BusinessException(IMPORT_FILE_UNREADABLE, "Could not parse file: " + e.getMessage());
        }
    }

    private List<ParsedRow> readXlsxRows(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[2];
                for (int col = 0; col < 2; col++) {
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
                String[] cells = new String[2];
                for (int col = 0; col < 2; col++) {
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
            Sheet sheet = workbook.createSheet("Practice tests");
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
