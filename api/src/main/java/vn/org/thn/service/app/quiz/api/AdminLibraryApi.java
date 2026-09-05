package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
 * Admin management of the textbook PDF library (2026-09-05, "thu vien sach giao khoa" feature) -
 * full CRUD, no root restriction (unlike {@code AdminManageApi}'s Admin-manages-Admin feature,
 * every Admin can manage textbooks). See {@link LibraryService}'s javadoc for the full 3-role
 * access model. Behind {@link JwtAuthFilter} under {@code /api/admin/*}.
 */
@Tag(name = "Admin - Library", description = "Admin CRUD for the textbook PDF library")
@RestController
@RequestMapping("/api/admin/library")
public class AdminLibraryApi extends BaseCtl {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private LibraryImportService libraryImportService;

    @Operation(
            summary = "List/search library documents",
            description = "Every filter is optional and AND-combined: grade (exact), subjectName (partial match), curriculum (exact, one of the Admin-managed curriculum list - see /api/admin/curricula)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching library documents")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<LibraryDocumentResponse>>> list(
            @Parameter(description = "Grade 1-12, exact match") @RequestParam(required = false) Integer grade,
            @Parameter(description = "Subject name, partial match") @RequestParam(required = false) String subjectName,
            @Parameter(description = "Curriculum, exact match") @RequestParam(required = false) String curriculum) {
        return ok(libraryService.list(grade, subjectName, curriculum));
    }

    @Operation(
            summary = "Upload a new textbook PDF",
            description = "grade must be 1-12 and curriculum must be a known name from /api/admin/curricula - otherwise QUIZ_032. PDF only, 50MB max. title is optional (a default is generated from subjectName/grade/volume/curriculum when left blank)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Uploaded successfully - returns the new library document"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid grade/curriculum - QUIZ_032, wrong file type - QUIZ_033, or file too large - QUIZ_034")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LibraryDocumentResponse>> upload(
            @Parameter(description = "Grade 1-12") @RequestParam int grade,
            @Parameter(description = "Subject name, e.g. \"Toan\"") @RequestParam String subjectName,
            @Parameter(description = "Curriculum - one of the fixed 3-value list") @RequestParam String curriculum,
            @Parameter(description = "Volume, e.g. \"Tap 1\" - optional") @RequestParam(required = false) String volume,
            @Parameter(description = "Display title - optional, a default is generated when left blank") @RequestParam(required = false) String title,
            @Parameter(description = "The PDF file") @RequestPart MultipartFile file) {
        return ok(libraryService.upload(grade, subjectName, curriculum, volume, title, file));
    }

    @Operation(
            summary = "Delete a library document",
            description = "Also deletes its PDF file and every Subject's link to it (cascade)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No library document with this id - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Library document id") @PathVariable Long id) {
        libraryService.delete(id);
        return ok();
    }

    @Operation(
            summary = "View/download a library document's PDF",
            description = "Admin has full access to the whole library, no ownership check (see LibraryService's javadoc)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the PDF file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No library document with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@Parameter(description = "Library document id") @PathVariable Long id) {
        LibraryFile file = libraryService.downloadForAdmin(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }

    @Operation(
            summary = "Download the library import template",
            description = "Returns a ready-to-fill Excel (default) or CSV file with the fixed 5-column layout (Lop/Mon hoc/Bo sach/Tap/Tieu de) plus one illustrative example row, which the import endpoint recognizes and skips automatically whether or not it is deleted before uploading."
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
            summary = "Bulk-import library documents (metadata only) from an Excel/CSV file",
            description = "Best-effort per row - one bad row does not stop the others in the same file. Every row creates a metadata-only document (no PDF yet, see LibraryService#createMetadataOnly) - upload each row's actual PDF afterward via PUT /{id}/file. A row that exactly duplicates an existing grade+subjectName+curriculum+volume combination is reported as an error and skipped."
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

    @Operation(
            summary = "Attach (or replace) a library document's PDF file",
            description = "Used to upload the actual PDF for a metadata-only row created via import (LibraryDocumentResponse#hasFile is false), but also works as a general \"replace the PDF\" action for a row that already has one - the old file (if any) is deleted from disk first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attached successfully - returns the updated library document (hasFile=true)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Wrong file type - QUIZ_033, or file too large - QUIZ_034"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No library document with this id - COMMON_005 NOT_FOUND")
    })
    @PutMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LibraryDocumentResponse>> attachFile(
            @Parameter(description = "Library document id") @PathVariable Long id,
            @Parameter(description = "The PDF file") @RequestPart MultipartFile file) {
        return ok(libraryService.attachFile(id, file));
    }
}
