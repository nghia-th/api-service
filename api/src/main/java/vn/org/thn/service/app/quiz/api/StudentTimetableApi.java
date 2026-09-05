package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
