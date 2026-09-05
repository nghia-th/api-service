package vn.org.thn.service.app.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Body of {@code PUT /api/parent/classrooms/{classroomId}/timetable/{dayOfWeek}} - REPLACES the
 * whole day's subject list in one call, not an incremental add/remove/reorder API - see {@code
 * TimetableService#setDay}'s javadoc for why a full-replace call is simpler and safer here than a
 * per-entry CRUD surface. {@code subjectIds} order becomes each new entry's {@code orderIndex}
 * (0-based), same "orderIndex from request list position" convention as {@code
 * TestCreateRequest#questionIds}. An empty list clears the whole day.
 * <p>
 * Renamed from {@code lessonIds} (2026-09-06 revision - see {@code TimetableEntry}'s javadoc) -
 * a day's schedule is now just an ordered list of Subjects, no specific Lesson pinned.
 */
@Data
public class TimetableDayRequest {

    @NotNull
    private List<Long> subjectIds;
}
