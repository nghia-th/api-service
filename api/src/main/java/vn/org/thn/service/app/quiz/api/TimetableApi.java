package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.LessonPreparationStatus;
import vn.org.thn.service.app.quiz.dto.TimetableDayRequest;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.LessonPreparationService;
import vn.org.thn.service.app.quiz.service.TimetableService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Parent-facing weekly timetable ("thoi khoa bieu") CRUD for one Classroom - part 1 of the
 * feature added 2026-09-05, per the user's explicit request "tao chuc nang thoi khoa bieu trong 1
 * tuan cua con". See {@code TimetableEntry}'s javadoc for the full design (single persistent
 * template, no time-of-day, pins an exact Lesson, no separate volume/tap field). Behind {@link
 * JwtAuthFilter} under {@code /api/parent/*}.
 */
@Tag(name = "Parent - Timetable", description = "Weekly timetable (thoi khoa bieu) for one Classroom")
@RestController
@RequestMapping("/api/parent")
public class TimetableApi extends BaseCtl {

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private LessonPreparationService lessonPreparationService;

    @Operation(
            summary = "The whole week's timetable for this classroom",
            description = "classroomId must belong to the current parent. Flat list mixing every dayOfWeek (1=Monday..7=Sunday), already sorted by dayOfWeek then orderIndex - group by dayOfWeek on the client."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every entry across the whole week")
    })
    @GetMapping("/classrooms/{classroomId}/timetable")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getWeek(@PathVariable Long classroomId) {
        return ok(timetableService.getWeek(classroomId));
    }

    @Operation(
            summary = "Replace one day's lesson list",
            description = "REPLACES every entry for this classroom+dayOfWeek in one call - lessonIds order becomes orderIndex (0-based). Pass an empty list to clear the day."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Day updated - returns the whole week again"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "dayOfWeek not in 1..7, or a lessonId does not belong to a subject in this classroom - COMMON_002")
    })
    @PutMapping("/classrooms/{classroomId}/timetable/{dayOfWeek}")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> setDay(
            @PathVariable Long classroomId,
            @Parameter(description = "1=Monday..7=Sunday") @PathVariable int dayOfWeek,
            @Valid @RequestBody TimetableDayRequest request) {
        timetableService.setDay(classroomId, dayOfWeek, request);
        return ok(timetableService.getWeek(classroomId));
    }

    @Operation(
            summary = "A student's tomorrow lesson-preparation checklist",
            description = "Item 10 of the 2026-09-05 batch request (\"phu huynh xem duoc con da chuan bi bai cho ngay mai hay chua\") - read-only, same shape as the student's own GET /api/student/preparation/tomorrow. studentId must belong to the current parent."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tomorrow's checklist for this student, each lesson flagged prepared/not"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This student does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/students/{studentId}/preparation/tomorrow")
    public ResponseEntity<ApiResponse<List<LessonPreparationStatus>>> getStudentTomorrowPreparation(@PathVariable Long studentId) {
        return ok(lessonPreparationService.getStudentTomorrowStatus(studentId));
    }
}
