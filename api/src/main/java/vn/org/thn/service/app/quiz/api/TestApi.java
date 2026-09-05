package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.PracticeGenerateRequest;
import vn.org.thn.service.app.quiz.dto.PracticeImportResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.app.quiz.dto.TestCreateRequest;
import vn.org.thn.service.app.quiz.dto.TestDetailResponse;
import vn.org.thn.service.app.quiz.dto.TestResponse;
import vn.org.thn.service.app.quiz.service.PracticeImportService;
import vn.org.thn.service.app.quiz.service.TestService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Test creation/listing/deletion for the currently logged-in Parent (task 5). Creating a test
 * assigns it immediately - there is no separate "assign" endpoint. Same conventions as {@link
 * QuestionApi} for auth/ownership/OpenAPI documentation.
 */
@Tag(name = "Test", description = "Create/list/delete Tests assigned by the current Parent to their own Students")
@RestController
@RequestMapping("/api/parent/tests")
public class TestApi extends BaseCtl {

    @Autowired
    private TestService testService;

    @Autowired
    private PracticeImportService practiceImportService;

    @Operation(
            summary = "Create and assign a test",
            description = "Creates a Test and assigns it to studentId in one call - status is ASSIGNED immediately, there is no separate assign step. questionIds' order becomes each question's display order."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Test"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field is missing, or questionIds is empty - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "studentId or one of questionIds belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student or question with the given id - COMMON_005 NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TestResponse>> create(@Valid @RequestBody TestCreateRequest request) {
        return ok(testService.create(request));
    }

    @Operation(
            summary = "Generate a practice test (Ôn tập)",
            description = "Picks random questions from the whole Subject's question pool (every Lesson under it) and creates a new Test tagged PRACTICE, assigned immediately (status ASSIGNED). Can be called again any number of times - each call creates a brand-new Test with a freshly-randomized question set; v1's 1-attempt-per-test rule is unaffected since a retake is always a new Test, never a new Attempt on the same Test."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new practice Test"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "The subject has no questions to practice from - QUIZ_018 SUBJECT_NO_QUESTIONS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "studentId/subjectId belongs to another parent, or the subject is not in the student's classroom - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student or subject with the given id - COMMON_005 NOT_FOUND")
    })
    @PostMapping("/practice")
    public ResponseEntity<ApiResponse<TestResponse>> generatePractice(@Valid @RequestBody PracticeGenerateRequest request) {
        return ok(testService.generatePractice(request));
    }

    @Operation(
            summary = "Download the practice-test bulk import template",
            description = "Returns a ready-to-fill Excel (default) or CSV file with the fixed 2-column layout (Ten dang nhap hoc sinh/So cau hoi, 2026-09-05 - Subject dropped from the file, chosen once as a query param on the import call itself instead) plus one illustrative example row, which the import endpoint recognizes and skips automatically whether or not it is deleted before uploading. Same shape as LessonApi#importTemplate."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the template file (Content-Type set per the requested format)")
    })
    @GetMapping("/practice/import-template")
    public ResponseEntity<byte[]> practiceImportTemplate(
            @Parameter(description = "\"xlsx\" (default) or \"csv\"") @RequestParam(required = false, defaultValue = "xlsx") String format) {
        TemplateFile template = practiceImportService.generateTemplate(format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.filename() + "\"")
                .body(template.content());
    }

    @Operation(
            summary = "Bulk-generate practice tests (Ôn tập) from an Excel/CSV file",
            description = "Best-effort per row - one bad row does not stop the others in the same file. subjectId is fixed for the WHOLE file (2026-09-05, per the user's clarification \"mỗi lần import một đề ôn theo môn\" - one import always targets one Subject) and is checked up front, before the file is even read - each row only names its own Student (by login username). Every row still gets a freshly-randomized question set from TestService#generatePractice, exactly like the single-call button - this is NOT a mode for hand-picking specific questions via the file."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File was read - check the response body for per-row errors, if any (this is 200 even when some/all rows failed, since the request itself succeeded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File could not be read at all (wrong format/corrupt/empty), or has more rows than the per-import limit - QUIZ_012 or QUIZ_011"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "subjectId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this subjectId - COMMON_005 NOT_FOUND")
    })
    @PostMapping(value = "/practice/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PracticeImportResponse>> practiceImportFile(
            @Parameter(description = "Subject id every imported practice test is generated from") @RequestParam Long subjectId,
            @Parameter(description = "The .xlsx or .csv file, filled in from the downloaded template") @RequestPart MultipartFile file) {
        return ok(practiceImportService.importFile(subjectId, file));
    }

    @Operation(
            summary = "List my tests",
            description = "Every Test belonging to the current Parent, optionally filtered to one student's tests. Not paginated in v1."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current parent's tests - never another parent's"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "studentId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this studentId - COMMON_005 NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<TestResponse>>> list(
            @Parameter(description = "Optional - narrows the list to this student's tests only") @RequestParam(required = false) Long studentId) {
        return ok(testService.list(studentId));
    }

    @Operation(
            summary = "Get one test's detail",
            description = "Returns the Test plus its full question list (with choices, including which one is correct) in display order."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Test with its questions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No test with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestDetailResponse>> get(@Parameter(description = "Test id") @PathVariable Long id) {
        return ok(testService.get(id));
    }

    @Operation(
            summary = "Delete a test",
            description = "Blocked once the test has any attempt (in progress or submitted), to avoid losing result history. Only the owning Parent can delete their own Test."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No test with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Test already has an attempt - QUIZ_009 TEST_HAS_ATTEMPTS")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Test id") @PathVariable Long id) {
        testService.delete(id);
        return ok();
    }
}
