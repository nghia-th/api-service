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
import vn.org.thn.service.app.quiz.dto.SubjectLessonReportStatus;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.LessonReportService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Student-facing "bao bai" (2026-09-06) - confirming which specific Lesson was studied today for
 * each Subject on today's timetable. Behind {@link JwtAuthFilter} under {@code /api/student/**}.
 * See {@link LessonReportService}'s javadoc for the full design (Lesson-level, backward-looking,
 * only today undoable, reported-forever exclusion).
 */
@Tag(name = "Student - Lesson report", description = "Confirm which lessons were actually studied today")
@RestController
@RequestMapping("/api/student/lesson-reports")
public class StudentLessonReportApi extends BaseCtl {

    @Autowired
    private LessonReportService lessonReportService;

    @Operation(summary = "Today's picker", description = "Every Subject on today's timetable, each with the Lessons already reported today and the Lessons still available to pick (never-reported ones only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Today's picker, in timetable order")
    })
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<SubjectLessonReportStatus>>> getTodayStatus() {
        return ok(lessonReportService.getMyTodayStatus());
    }

    @Operation(summary = "Report a lesson studied today", description = "Rejects a lesson whose subject is not on today's timetable, or one already reported on any date. Returns the updated picker.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reported - returns the whole updated picker"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "This lesson's subject is not on today's timetable - QUIZ_040"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This lesson has already been reported - QUIZ_041")
    })
    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<List<SubjectLessonReportStatus>>> reportLesson(@Parameter(description = "Lesson id, must belong to a subject on today's timetable") @PathVariable Long lessonId) {
        return ok(lessonReportService.reportLesson(lessonId));
    }

    @Operation(summary = "Undo today's report", description = "Only a lesson reported TODAY can be undone - a report from a previous day is permanent history. Idempotent if never reported. Returns the updated picker.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Undone - returns the whole updated picker"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This report is from a previous day and can no longer be undone - QUIZ_042")
    })
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<List<SubjectLessonReportStatus>>> unreportLesson(@Parameter(description = "Lesson id") @PathVariable Long lessonId) {
        return ok(lessonReportService.unreportLesson(lessonId));
    }
}
