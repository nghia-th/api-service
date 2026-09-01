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
import vn.org.thn.service.app.quiz.dto.LessonImage;
import vn.org.thn.service.app.quiz.dto.StudentLessonResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.StudentLessonService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

/**
 * Student-facing lesson content (task "Backend: Student xem lai noi dung bai hoc", 2026-09-01) -
 * lets a Student re-read the lesson material a Test's questions came from, both while taking the
 * test and after submitting (same take-test screen either way in v1). Under {@code
 * /api/student/**}, gated by {@link JwtAuthFilter} to a STUDENT-role token, mirroring {@link
 * StudentAttemptApi}. Access is per-lesson, not blanket - see {@link
 * StudentLessonService}'s javadoc for exactly which lessons a given Student may reach.
 */
@Tag(name = "Student Lesson", description = "Student-facing: read a lesson's content/image, only for lessons reachable from one of the student's assigned tests")
@RestController
@RequestMapping("/api/student/lessons")
public class StudentLessonApi extends BaseCtl {

    @Autowired
    private StudentLessonService studentLessonService;

    @Operation(
            summary = "Get a lesson's content",
            description = "Only reachable if some test assigned to the current student was built from a question belonging to this lesson."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the lesson's summary/content/textbookPage/hasImage"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This lesson is not reachable from any test assigned to the current student - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentLessonResponse>> get(@Parameter(description = "Lesson id") @PathVariable Long id) {
        return ok(studentLessonService.get(id));
    }

    @Operation(
            summary = "Download a lesson's illustrative image",
            description = "Same access rule as GET /api/student/lessons/{id}. See LessonApi for the parent-facing equivalent."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the image file (Content-Type set from its stored type)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This lesson is not reachable from any test assigned to the current student - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this id, or it has no image - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@Parameter(description = "Lesson id") @PathVariable Long id) {
        LessonImage image = studentLessonService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.filename() + "\"")
                .body(image.content());
    }
}
