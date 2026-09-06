package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;

import java.time.LocalDate;
import java.util.List;

/**
 * Student-facing "what am I studying today/tomorrow" view (item 5 of the 2026-09-05 batch request,
 * part 2 of the Timetable epic - see {@code claude/timetable-feature-2026-09-05.md}). Reads the
 * SAME {@link TimetableEntry} rows the Parent edits via {@link TimetableService}, filtered down to
 * the single day the Student asks about.
 * <p>
 * <b>Revision 2026-09-06 (b):</b> {@link TimetableEntry} moved from per-Classroom to per-Student
 * (see its javadoc) - this class used to look up the current Student's row purely to read its
 * {@code classroomId} before calling {@code TimetableService#getForClassroomAndDate}; now that
 * {@link TimetableService#getForStudentAndDate} takes the Student's own id directly, that lookup
 * (and this class's {@code StudentRepository} dependency) is no longer needed - an authenticated
 * Student session's {@link CurrentUser#get()} id IS the studentId to query with.
 * <p>
 * <b>Revision 2026-09-06 (c):</b> added {@link #getWeek} and {@link #addSubject} - the user's
 * explicit request "hoc sinh cho phep hoc sinh tao thoi khoa bieu khong cho xoa, update - viec xoa
 * hoac update thi phu huynh lam, sau khi hoc sinh them thoi khoa bieu thi phu huynh se thay" (a
 * Student may ADD a Subject to any day of their own week, but never remove/reorder/replace - that
 * stays exclusively the Parent's job via {@code TimetableApi#setDay}; whatever the Student adds is
 * simply visible the next time the Parent loads the week, no separate approval/notify step).
 * <p>
 * Reuses {@link TimetableService#toResponses} rather than re-implementing the TimetableEntry ->
 * Subject name-resolution walk a second time.
 */
@Service
public class StudentTimetableService extends IBase {

    @Autowired
    private TimetableService timetableService;

    public List<TimetableEntryResponse> getToday() {
        return getForDate(LocalDate.now());
    }

    public List<TimetableEntryResponse> getTomorrow() {
        return getForDate(LocalDate.now().plusDays(1));
    }

    /**
     * {@code date.getDayOfWeek().getValue()} is already 1=Monday..7=Sunday (ISO-8601) - exactly
     * the convention {@link TimetableEntry#getDayOfWeek()} stores, so no conversion is needed here
     * (unlike the frontend's JS {@code Date.getDay()}, which is 0=Sunday-based and must convert -
     * see the frontend timetable code's own comment on that). {@code LocalDate#plusDays} handles
     * week wraparound on its own (Sunday + 1 day correctly becomes next Monday), so {@link
     * #getTomorrow()} needs no manual modulo.
     */
    private List<TimetableEntryResponse> getForDate(LocalDate date) {
        Long studentId = CurrentUser.get().userId();
        return timetableService.getForStudentAndDate(studentId, date);
    }

    /** The whole week for the current Student, sorted by dayOfWeek then orderIndex - same flat-list shape as the Parent-facing {@code TimetableService#getWeek}, for the new "Student creates their own timetable" page (2026-09-06). */
    public List<TimetableEntryResponse> getWeek() {
        Long studentId = CurrentUser.get().userId();
        return timetableService.getOwnWeek(studentId);
    }

    /** Appends {@code subjectId} to {@code dayOfWeek}'s list for the current Student's own timetable - see {@link TimetableService#addOwnEntry}'s javadoc for the full ADD-only rule. Returns the updated whole week (same "write endpoint returns the fresh whole collection" convention as every other timetable/preparation write in this codebase). */
    public List<TimetableEntryResponse> addSubject(int dayOfWeek, Long subjectId) {
        Long studentId = CurrentUser.get().userId();
        timetableService.addOwnEntry(studentId, dayOfWeek, subjectId);
        return getWeek();
    }
}
