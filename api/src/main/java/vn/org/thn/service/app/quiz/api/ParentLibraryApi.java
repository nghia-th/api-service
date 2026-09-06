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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentResponse;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.SubjectLibraryLinkResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.ParentLibraryService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Parent-facing access to the Admin-managed library (2026-09-05, "thu vien sach giao khoa"
 * feature; extended 2026-09-06 to also cover general "mon hoc" entries with multiple files each) -
 * browse the whole catalog, link/unlink own Subjects, download a linked entry's files. See {@link
 * ParentLibraryService}'s javadoc for the full access model. Behind {@link JwtAuthFilter} under
 * {@code /api/parent/*}.
 */
@Tag(name = "Parent - Library", description = "Parent browsing, linking and downloading of the library")
@RestController
@RequestMapping("/api/parent")
public class ParentLibraryApi extends BaseCtl {

    @Autowired
    private ParentLibraryService parentLibraryService;

    @Operation(
            summary = "Browse the whole library",
            description = "Read-only, no ownership filtering - every Parent sees the same whole catalog, to decide what to link. Same filters as the Admin listing."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching library entries, each with its file list")
    })
    @GetMapping("/library")
    public ResponseEntity<ApiResponse<List<LibraryDocumentResponse>>> browse(
            @Parameter(description = "Grade 1-12, exact match") @RequestParam(required = false) Integer grade,
            @Parameter(description = "Subject name, partial match") @RequestParam(required = false) String subjectName,
            @Parameter(description = "Curriculum, exact match") @RequestParam(required = false) String curriculum) {
        return ok(parentLibraryService.browse(grade, subjectName, curriculum));
    }

    @Operation(
            summary = "List entries linked to a subject",
            description = "subjectId must belong to the current parent."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entries currently linked to this subject, each with its file list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/subjects/{subjectId}/library-links")
    public ResponseEntity<ApiResponse<List<SubjectLibraryLinkResponse>>> listLinks(@Parameter(description = "Subject id") @PathVariable Long subjectId) {
        return ok(parentLibraryService.listLinks(subjectId));
    }

    @Operation(
            summary = "Link a subject to a library entry",
            description = "subjectId must belong to the current parent, documentId must exist. One subject can link multiple entries."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Linked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject or no entry with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Already linked - QUIZ_035 LIBRARY_ALREADY_LINKED")
    })
    @PostMapping("/subjects/{subjectId}/library-links/{documentId}")
    public ResponseEntity<ApiResponse<SubjectLibraryLinkResponse>> link(
            @Parameter(description = "Subject id") @PathVariable Long subjectId,
            @Parameter(description = "Library entry id") @PathVariable Long documentId) {
        return ok(parentLibraryService.link(subjectId, documentId));
    }

    @Operation(
            summary = "Unlink a subject from a library entry",
            description = "subjectId must belong to the current parent."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unlinked successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id, or the link does not exist - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/subjects/{subjectId}/library-links/{documentId}")
    public ResponseEntity<ApiResponse<Void>> unlink(
            @Parameter(description = "Subject id") @PathVariable Long subjectId,
            @Parameter(description = "Library entry id") @PathVariable Long documentId) {
        parentLibraryService.unlink(subjectId, documentId);
        return ok();
    }

    @Operation(
            summary = "Download one of a linked entry's files",
            description = "subjectId must belong to the current parent AND already be linked to documentId. fileId must belong to documentId (revision 2026-09-06 - an entry may now carry multiple files, see the entry's own 'files' list from the endpoints above for valid ids)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent, or is not linked to this entry - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject/entry/file with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/subjects/{subjectId}/library-links/{documentId}/files/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Subject id") @PathVariable Long subjectId,
            @Parameter(description = "Library entry id") @PathVariable Long documentId,
            @Parameter(description = "File id") @PathVariable Long fileId) {
        LibraryFile file = parentLibraryService.downloadFile(subjectId, documentId, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }
}
