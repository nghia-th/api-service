package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.TimetableEntry;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.TimetableEntryRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Student-facing "what am I studying today/tomorrow" view (item 5 of the 2026-09-05 batch request,
 * part 2 of the Timetable epic - see {@code claude/timetable-feature-2026-09-05.md}). Reads the
 * SAME {@link TimetableEntry} rows the Parent edits via {@link TimetableService}, filtered down to
 * the single day the Student asks about.
 * <p>
 * A Student always belongs to exactly 1 {@link vn.org.thn.service.app.quiz.entity.Classroom} (see
 * {@link Student#getClassroomId()}), so unlike the Parent-facing API there is no classroomId
 * parameter anywhere here - it is resolved from the Student's own row every call, the same "read
 * {@link CurrentUser#get()} yourself, never trust a caller-supplied id for ownership" shape as
 * every other Student/Parent service in this codebase.
 * <p>
 * Reuses {@link TimetableService#toResponses} rather than re-implementing the TimetableEntry ->
 * Lesson -> Subject name-resolution walk a second time.
 */
@Service
public class StudentTimetableService extends IBase {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

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
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }

        int dayOfWeek = date.getDayOfWeek().getValue();
        List<TimetableEntry> entries = timetableEntryRepository.query()
                .eq(TimetableEntry::getClassroomId, student.getClassroomId())
                .eq(TimetableEntry::getDayOfWeek, dayOfWeek)
                .list();
        entries.sort(Comparator.comparing(TimetableEntry::getOrderIndex));

        return timetableService.toResponses(entries);
    }
}
