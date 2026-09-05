package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.LessonReportHistoryItem;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.LessonReportService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Parent-facing read-only "bao bai" history (2026-09-06 - "ben phu huynh co the xem duoc hom nay
 * con hoc gi va cung co the xem lai nhung ngay truoc con da chon"). Behind {@link JwtAuthFilter}
 * under {@code /api/parent/*}. {@code studentId} must belong to the current parent.
 */
@Tag(name = "Parent - Lesson report", description = "Read-only history of which lessons a student reported studying, by date")
@RestController
@RequestMapping("/api/parent")
public class LessonReportApi extends BaseCtl {

    @Autowired
    private LessonReportService lessonReportService;

    @Operation(
            summary = "A student's reported lessons for one date",
            description = "Defaults to today when date is omitted. subjectId optionally narrows to one Subject - omit to see every Subject reported that date."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every lesson reported on this date (optionally filtered by subject), sorted by subject then lesson"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This student does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/students/{studentId}/lesson-reports")
    public ResponseEntity<ApiResponse<List<LessonReportHistoryItem>>> getStudentHistory(
            @PathVariable Long studentId,
            @Parameter(description = "Date to view (yyyy-MM-dd) - defaults to today if omitted") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Optional subject id filter") @RequestParam(required = false) Long subjectId) {
        return ok(lessonReportService.getStudentHistory(studentId, date, subjectId));
    }
}
