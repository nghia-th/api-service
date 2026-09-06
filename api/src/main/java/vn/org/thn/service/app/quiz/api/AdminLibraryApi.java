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
import vn.org.thn.service.app.quiz.dto.LibraryDocumentCreateRequest;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentFileResponse;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentResponse;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.LibraryImportResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.LibraryImportService;
import vn.org.thn.service.app.quiz.service.LibraryService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Admin management of the library (2026-09-05, "thu vien sach giao khoa" feature; extended
 * 2026-09-06 to also cover general "mon hoc" entries with no grade/curriculum and multiple files
 * per entry - see {@link LibraryService}'s javadoc) - full CRUD, no root restriction. Behind
 * {@link JwtAuthFilter} under {@code /api/admin/*}.
 */
@Tag(name = "Admin - Library", description = "Admin CRUD for the library (textbooks and general courses), and per-entry file management")
@RestController
@RequestMapping("/api/admin/library")
public class AdminLibraryApi extends BaseCtl {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private LibraryImportService libraryImportService;

    @Operation(
            summary = "List/search library entries",
            description = "Every filter is optional and AND-combined: grade (exact), subjectName (partial match), curriculum (exact, one of the Admin-managed curriculum list - see /api/admin/curricula)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching library entries, each with its file list")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<LibraryDocumentResponse>>> list(
            @Parameter(description = "Grade 1-12, exact match") @RequestParam(required = false) Integer grade,
            @Parameter(description = "Subject name, partial match") @RequestParam(required = false) String subjectName,
            @Parameter(description = "Curriculum, exact match") @RequestParam(required = false) String curriculum) {
        return ok(libraryService.list(grade, subjectName, curriculum));
    }

    @Operation(
            summary = "Create a new library entry",
            description = "grade/curriculum are both optional - a general course not tied to any grade/curriculum (e.g. \"Lap trinh Python\") leaves both out. When provided, grade must be 1-12 and curriculum must be a known name from /api/admin/curricula - otherwise QUIZ_032. No file here - add files afterward via POST /{id}/files, any number of them."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new entry (files=[])"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid grade/curriculum - QUIZ_032, or subjectName missing - COMMON_001")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<LibraryDocumentResponse>> create(@Valid @RequestBody LibraryDocumentCreateRequest request) {
        return ok(libraryService.create(request.getGrade(), request.getSubjectName(), request.getCurriculum(), request.getVolume(), request.getTitle()));
    }

    @Operation(
            summary = "Delete a library entry",
            description = "Also deletes every one of its files and every Subject's link to it (cascade)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No library entry with this id - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Library entry id") @PathVariable Long id) {
        libraryService.delete(id);
        return ok();
    }

    @Operation(
            summary = "Add a file to a library entry",
            description = "PDF or PowerPoint (.ppt/.pptx), 50MB max. Never replaces an existing file - a document can now carry any number of them (revision 2026-09-06); remove one explicitly via DELETE /{id}/files/{fileId} first if replacing."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Added successfully - returns the new file's metadata"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Wrong file type - QUIZ_033, or file too large - QUIZ_034"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No library entry with this id - COMMON_005 NOT_FOUND")
    })
    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LibraryDocumentFileResponse>> addFile(
            @Parameter(description = "Library entry id") @PathVariable Long id,
            @Parameter(description = "The PDF or PowerPoint file") @RequestPart MultipartFile file) {
        return ok(libraryService.addFile(id, file));
    }

    @Operation(
            summary = "Remove a file from a library entry",
            description = "Deletes just this one file - the entry and its other files are untouched."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Removed successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No such file on this library entry - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> removeFile(
            @Parameter(description = "Library entry id") @PathVariable Long id,
            @Parameter(description = "File id") @PathVariable Long fileId) {
        libraryService.removeFile(id, fileId);
        return ok();
    }

    @Operation(
            summary = "View/download one of a library entry's files",
            description = "Admin has full access to the whole library, no ownership check (see LibraryService's javadoc)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No such file on this library entry - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<byte[]> file(
            @Parameter(description = "Library entry id") @PathVariable Long id,
            @Parameter(description = "File id") @PathVariable Long fileId) {
        LibraryFile file = libraryService.downloadForAdmin(id, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }

    @Operation(
            summary = "Download the library import template",
            description = "Returns a ready-to-fill Excel (default) or CSV file with the fixed 5-column layout (Lop/Mon hoc/Bo sach/Tap/Tieu de - Lop and Bo sach are optional) plus 2 illustrative example rows, which the import endpoint recognizes and skips automatically whether or not they are deleted before uploading."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the template file (Content-Type set per the requested format)")
    })
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate(
            @Parameter(description = "\"xlsx\" (default) or \"csv\"") @RequestParam(required = false, defaultValue = "xlsx") String format) {
        TemplateFile template = libraryImportService.generateTemplate(format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.filename() + "\"")
                .body(template.content());
    }

    @Operation(
            summary = "Bulk-import library entries (metadata only) from an Excel/CSV file",
            description = "Best-effort per row - one bad row does not stop the others in the same file. Every row creates an entry with no files yet - add each row's file(s) afterward via POST /{id}/files. Lop/Bo sach columns may be left blank for a general course entry. A row that exactly duplicates an existing grade+subjectName+curriculum+volume combination is reported as an error and skipped."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File was read - check the response body for per-row errors, if any (this is 200 even when some/all rows failed, since the request itself succeeded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File could not be read at all (wrong format/corrupt/empty), or has more rows than the per-import limit - QUIZ_012 or QUIZ_011")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LibraryImportResponse>> importFile(
            @Parameter(description = "The .xlsx or .csv file, filled in from the downloaded template") @RequestPart MultipartFile file) {
        return ok(libraryImportService.importFile(file));
    }
}
