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
 * Student-facing read-only access to documents linked to a subject in their own classroom
 * (2026-09-05, "thu vien sach giao khoa" feature) - see {@link StudentLibraryService}'s javadoc
 * for the full access model. Behind {@link JwtAuthFilter} under {@code /api/student/*}.
 */
@Tag(name = "Student - Library", description = "Student viewing/downloading of textbook library documents linked to their own classroom's subjects")
@RestController
@RequestMapping("/api/student")
public class StudentLibraryApi extends BaseCtl {

    @Autowired
    private StudentLibraryService studentLibraryService;

    @Operation(
            summary = "List documents linked to a subject",
            description = "subjectId must be in the current student's own classroom."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documents currently linked to this subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject is not in the current student's classroom - COMMON_004 FORBIDDEN")
    })
    @GetMapping("/subjects/{subjectId}/library-links")
    public ResponseEntity<ApiResponse<List<SubjectLibraryLinkResponse>>> listLinks(@Parameter(description = "Subject id") @PathVariable Long subjectId) {
        return ok(studentLibraryService.listLinks(subjectId));
    }

    @Operation(
            summary = "Download a linked library document's PDF",
            description = "subjectId must be in the current student's own classroom AND already be linked to documentId."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the PDF file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject is not in the current student's classroom, or is not linked to this document - COMMON_004 FORBIDDEN")
    })
    @GetMapping("/subjects/{subjectId}/library-links/{documentId}/file")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Subject id") @PathVariable Long subjectId,
            @Parameter(description = "Library document id") @PathVariable Long documentId) {
        LibraryFile file = studentLibraryService.downloadFile(subjectId, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }
}
