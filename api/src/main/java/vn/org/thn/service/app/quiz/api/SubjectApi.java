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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.BulkDeleteResponse;
import vn.org.thn.service.app.quiz.dto.SubjectImportResponse;
import vn.org.thn.service.app.quiz.dto.SubjectRequest;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.SubjectImportService;
import vn.org.thn.service.app.quiz.service.SubjectService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Subject CRUD for the currently logged-in Parent (task 3). Same conventions as {@link
 * vn.org.thn.service.app.quiz.api.StudentApi}: behind {@link JwtAuthFilter}, ownership enforced
 * in the service layer, Swagger {@code @io.swagger.v3.oas.annotations.responses.ApiResponse}
 * fully-qualified to avoid colliding with this module's own {@link ApiResponse}.
 */
@Tag(name = "Subject", description = "CRUD for the current Parent's own Subjects")
@RestController
@RequestMapping("/api/parent/subjects")
public class SubjectApi extends BaseCtl {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectImportService subjectImportService;

    @Operation(
            summary = "Create a subject",
            description = "Creates a new Subject owned by the current Parent. parentId is taken from the token, never from the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        return ok(subjectService.create(request));
    }

    @Operation(
            summary = "Update a subject",
            description = "Only the owning Parent can update their own Subject."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated Subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @Parameter(description = "Subject id") @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return ok(subjectService.update(id, request));
    }

    @Operation(
            summary = "List my subjects",
            description = "Every Subject belonging to the current Parent, optionally narrowed to one Classroom via ?classroomId=. Not paginated in v1."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current parent's subjects - never another parent's"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "classroomId was supplied but does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "classroomId was supplied but no classroom exists with that id - COMMON_005 NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> list(
            @Parameter(description = "Optional Classroom id to narrow the list to") @org.springframework.web.bind.annotation.RequestParam(required = false) Long classroomId) {
        return ok(subjectService.list(classroomId));
    }

    @Operation(
            summary = "Get one subject",
            description = "Only the owning Parent can view their own Subject."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> get(@Parameter(description = "Subject id") @PathVariable Long id) {
        return ok(subjectService.get(id));
    }

    @Operation(
            summary = "Delete a subject",
            description = "CASCADES (2026-09-06 revision) - deletes every Lesson/Question/Test that depends on this Subject, including any Test's Attempt/score history, plus its Timetable/lesson-preparation/library-link rows. No longer blocked by Lesson children (was QUIZ_005 - see CascadeDeleteService's javadoc). Only the owning Parent can delete their own Subject. Irreversible."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully (with everything that depended on it) - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Subject id") @PathVariable Long id) {
        subjectService.delete(id);
        return ok();
    }

    @Operation(
            summary = "Bulk-delete subjects",
            description = "Best-effort - deletes every id in the request body via the same cascade as the single-delete endpoint above; one id failing (wrong owner, already gone, ...) does not stop the rest. Used for both \"delete selected\" (a subset of ids) and \"delete all\" (every id currently listed) from the frontend, which already scopes the ids to one Classroom filter before calling this."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request processed - check the response body for per-id errors, if any (this is 200 even when some/all ids failed, since the request itself succeeded)")
    })
    @DeleteMapping("/deletes")
    public ResponseEntity<ApiResponse<BulkDeleteResponse>> deleteMany(@RequestBody List<Long> ids) {
        return ok(subjectService.deleteMany(ids));
    }

    @Operation(
            summary = "Download the subject import template",
            description = "Returns a ready-to-fill Excel (default) or CSV file with a single 'Ten mon hoc' column plus one illustrative example row, which the import endpoint recognizes and skips automatically whether or not it is deleted before uploading."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the template file (Content-Type set per the requested format)")
    })
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate(
            @Parameter(description = "\"xlsx\" (default) or \"csv\"") @RequestParam(required = false, defaultValue = "xlsx") String format) {
        TemplateFile template = subjectImportService.generateTemplate(format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.filename() + "\"")
                .body(template.content());
    }

    @Operation(
            summary = "Bulk-import subjects from an Excel/CSV file",
            description = "Best-effort per row - one bad row does not stop the others in the same file. classroomId must belong to the current parent, checked before the file is even read. A row whose name already exists in this classroom is reported as an error and skipped."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File was read - check the response body for per-row errors, if any (this is 200 even when some/all rows failed, since the request itself succeeded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File could not be read at all (wrong format/corrupt/empty), or has more rows than the per-import limit - QUIZ_012 or QUIZ_011"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "classroomId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No classroom with this classroomId - COMMON_005 NOT_FOUND")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubjectImportResponse>> importFile(
            @Parameter(description = "Classroom id every imported subject is attached to") @RequestParam Long classroomId,
            @Parameter(description = "The .xlsx or .csv file, filled in from the downloaded template") @RequestPart MultipartFile file) {
        return ok(subjectImportService.importFile(classroomId, file));
    }
}
