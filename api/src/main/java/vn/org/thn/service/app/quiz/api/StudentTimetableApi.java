package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.StudentTimetableService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Student-facing "hom nay/ngay mai hoc mon gi, bai gi" view (item 5 of the 2026-09-05 batch
 * request, part 2 of the Timetable epic). Behind {@link JwtAuthFilter} under {@code
 * /api/student/**}, mirroring {@link StudentLessonApi}/{@link StudentAttemptApi}. Always resolves
 * the current Student's own single Classroom - there is no classroomId path/query parameter, see
 * {@link StudentTimetableService}'s javadoc.
 * <p>
 * <b>Revision 2026-09-06 (c):</b> added {@link #week} and {@link #addSubject} for the new
 * "Student creates their own timetable" page - {@code GET /week} (whole week, read-only) and
 * {@code POST /{dayOfWeek}/subjects/{subjectId}} (ADD ONLY - see {@link
 * StudentTimetableService}'s javadoc for why there is deliberately no matching DELETE/PUT here;
 * removing or reordering an entry stays exclusively the Parent's job via {@code
 * TimetableApi#setDay}). Path shape matches the existing {@code
 * StudentPreparationApi}'s own {@code PUT .../subjects/{subjectId}} convention (subjectId as a
 * path variable, no request body needed for a single id).
 */
@Tag(name = "Student - Timetable", description = "What the current student is studying today/tomorrow")
@RestController
@RequestMapping("/api/student/timetable")
public class StudentTimetableApi extends BaseCtl {

    @Autowired
    private StudentTimetableService studentTimetableService;

    @Operation(summary = "Today's lessons", description = "Ordered list of lessons scheduled for today in the current student's classroom timetable. Empty list if none set.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Today's timetable entries, in order")
    })
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> today() {
        return ok(studentTimetableService.getToday());
    }

    @Operation(summary = "Tomorrow's lessons", description = "Same as GET /today but for tomorrow - used by the student to prepare for the next day.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tomorrow's timetable entries, in order")
    })
    @GetMapping("/tomorrow")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> tomorrow() {
        return ok(studentTimetableService.getTomorrow());
    }

    @Operation(
            summary = "The whole week's timetable (read-only)",
            description = "Same flat-list shape as the Parent-facing GET /api/parent/students/{studentId}/timetable, for the current student's own timetable."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every entry across the whole week")
    })
    @GetMapping("/week")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> week() {
        return ok(studentTimetableService.getWeek());
    }

    @Operation(
            summary = "Add a subject to one day (ADD ONLY - never edit/delete)",
            description = "Appends subjectId to the end of dayOfWeek's list for the current student's own timetable - idempotent, a no-op if already present. There is deliberately no matching DELETE/PUT here: removing, reordering or replacing a day stays exclusively the Parent's job (see TimetableApi#setDay) - the Parent simply sees whatever the student adds the next time they load the week."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Added (or already present) - returns the whole updated week"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "dayOfWeek not in 1..7, or subjectId does not belong to this student's classroom - COMMON_002")
    })
    @PostMapping("/{dayOfWeek}/subjects/{subjectId}")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> addSubject(
            @Parameter(description = "1=Monday..7=Sunday") @PathVariable int dayOfWeek,
            @PathVariable Long subjectId) {
        return ok(studentTimetableService.addSubject(dayOfWeek, subjectId));
    }
}
