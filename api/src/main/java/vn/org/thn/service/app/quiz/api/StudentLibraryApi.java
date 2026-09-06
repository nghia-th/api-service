package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.SubjectLibraryLinkResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.StudentLibraryService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Student-facing read-only access to entries linked to a subject in their own classroom
 * (2026-09-05, "thu vien sach giao khoa" feature; extended 2026-09-06 for multi-file entries) -
 * see {@link StudentLibraryService}'s javadoc for the full access model. Behind {@link
 * JwtAuthFilter} under {@code /api/student/*}.
 */
@Tag(name = "Student - Library", description = "Student viewing/downloading of library entries linked to their own classroom's subjects")
@RestController
@RequestMapping("/api/student")
public class StudentLibraryApi extends BaseCtl {

    @Autowired
    private StudentLibraryService studentLibraryService;

    @Operation(
            summary = "List entries linked to a subject",
            description = "subjectId must be in the current student's own classroom."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entries currently linked to this subject, each with its file list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject is not in the current student's classroom - COMMON_004 FORBIDDEN")
    })
    @GetMapping("/subjects/{subjectId}/library-links")
    public ResponseEntity<ApiResponse<List<SubjectLibraryLinkResponse>>> listLinks(@Parameter(description = "Subject id") @PathVariable Long subjectId) {
        return ok(studentLibraryService.listLinks(subjectId));
    }

    @Operation(
            summary = "Download one of a linked entry's files",
            description = "subjectId must be in the current student's own classroom AND already be linked to documentId. fileId must belong to documentId."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject is not in the current student's classroom, or is not linked to this entry - COMMON_004 FORBIDDEN")
    })
    @GetMapping("/subjects/{subjectId}/library-links/{documentId}/files/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Subject id") @PathVariable Long subjectId,
            @Parameter(description = "Library entry id") @PathVariable Long documentId,
            @Parameter(description = "File id") @PathVariable Long fileId) {
        LibraryFile file = studentLibraryService.downloadFile(subjectId, documentId, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }
}
