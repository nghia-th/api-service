package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.LessonPreparationStatus;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.LessonPreparationService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Student-facing "prepared for tomorrow" checklist (item 9 of the 2026-09-05 batch request).
 * Behind {@link JwtAuthFilter} under {@code /api/student/**}. Every endpoint is scoped to
 * tomorrow only - see {@link LessonPreparationService}'s javadoc for why there is no date
 * parameter anywhere here.
 * <p>
 * Path renamed from {@code .../tomorrow/lessons/{lessonId}} to {@code
 * .../tomorrow/subjects/{subjectId}} (2026-09-06 revision) - the checklist tracks Subjects now,
 * not Lessons, see {@code LessonPreparation}'s javadoc.
 */
@Tag(name = "Student - Lesson preparation", description = "Mark tomorrow's subjects as prepared")
@RestController
@RequestMapping("/api/student/preparation")
public class StudentPreparationApi extends BaseCtl {

    @Autowired
    private LessonPreparationService lessonPreparationService;

    @Operation(summary = "Tomorrow's checklist", description = "Every subject on tomorrow's timetable for the current student's classroom, each flagged whether already marked prepared.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tomorrow's checklist, in timetable order")
    })
    @GetMapping("/tomorrow")
    public ResponseEntity<ApiResponse<List<LessonPreparationStatus>>> getTomorrowStatus() {
        return ok(lessonPreparationService.getMyTomorrowStatus());
    }

    @Operation(summary = "Mark a subject prepared", description = "Idempotent - marking an already-prepared subject again is a no-op. Returns the updated checklist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Marked - returns the whole updated checklist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "subjectId is not on tomorrow's timetable for this student - COMMON_002")
    })
    @PutMapping("/tomorrow/subjects/{subjectId}")
    public ResponseEntity<ApiResponse<List<LessonPreparationStatus>>> markPrepared(@Parameter(description = "Subject id, must be on tomorrow's timetable") @PathVariable Long subjectId) {
        lessonPreparationService.markPrepared(subjectId);
        return ok(lessonPreparationService.getMyTomorrowStatus());
    }

    @Operation(summary = "Un-mark a subject", description = "Idempotent - no error if it was not marked. Returns the updated checklist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Un-marked - returns the whole updated checklist")
    })
    @DeleteMapping("/tomorrow/subjects/{subjectId}")
    public ResponseEntity<ApiResponse<List<LessonPreparationStatus>>> unmarkPrepared(@Parameter(description = "Subject id") @PathVariable Long subjectId) {
        lessonPreparationService.unmarkPrepared(subjectId);
        return ok(lessonPreparationService.getMyTomorrowStatus());
    }
}
