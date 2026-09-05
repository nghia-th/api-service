package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.TimetableDayRequest;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.entity.TimetableEntry;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.repository.TimetableEntryRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Weekly timetable ("thoi khoa bieu") CRUD for the currently logged-in Parent's Classroom (part 1
 * of the feature added 2026-09-05, per the user's explicit request "tao chuc nang thoi khoa bieu
 * trong 1 tuan cua con"). Later parts (Student "today/tomorrow" view, Parent weekly view, the
 * lesson-preparation checkbox) all read {@link #getWeek} rather than duplicating this class's own
 * lookup logic. See {@link TimetableEntry}'s javadoc for the full design rationale (single
 * persistent template, no time-of-day, pins an exact Lesson, no separate volume/tap field).
 * <p>
 * Same "read {@link CurrentUser#get()} itself, ownership enforced here rather than trusted from
 * the caller" shape as every other Parent-facing service in this codebase.
 */
@Service
public class TimetableService extends IBase {

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassroomService classroomService;

    /** The whole week for {@code classroomId}, sorted by dayOfWeek then orderIndex - a flat list, group by dayOfWeek on the client (same "flat list, group on the client" shape as {@code StudentTestSummaryResponse}). */
    public List<TimetableEntryResponse> getWeek(Long classroomId) {
        Long parentId = CurrentUser.get().userId();
        classroomService.getOwnedOrThrow(classroomId, parentId);

        List<TimetableEntry> entries = timetableEntryRepository.query()
                .eq(TimetableEntry::getClassroomId, classroomId).list();
        entries.sort(Comparator.comparing(TimetableEntry::getDayOfWeek)
                .thenComparing(TimetableEntry::getOrderIndex));

        return entries.stream().map(entry -> {
            Lesson lesson = lessonRepository.findById(entry.getLessonId());
            Subject subject = lesson == null ? null : subjectRepository.findById(lesson.getSubjectId());
            return TimetableEntryResponse.from(entry, lesson, subject);
        }).toList();
    }

    /**
     * REPLACES every {@link TimetableEntry} for {@code classroomId}+{@code dayOfWeek} with the
     * lessons in {@code request.getLessonIds()}, in that order (0-based {@code orderIndex}) - see
     * {@code TimetableDayRequest}'s javadoc for why a full-replace call was chosen over a
     * per-entry add/remove/reorder API (a Parent editing "Monday's schedule" naturally thinks of
     * it as "here is the new full list for Monday", not a sequence of individual inserts/deletes -
     * and a full replace can never leave stray leftover rows from a previous edit).
     * <p>
     * Every {@code lessonId} is validated BEFORE anything is deleted, so a bad id (unknown, or
     * belonging to a Subject outside this Classroom) leaves the day completely untouched rather
     * than half-cleared.
     */
    @Transactional
    public void setDay(Long classroomId, int dayOfWeek, TimetableDayRequest request) {
        Long parentId = CurrentUser.get().userId();
        classroomService.getOwnedOrThrow(classroomId, parentId);
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                    "dayOfWeek must be between 1 (Monday) and 7 (Sunday)");
        }

        List<Lesson> lessons = new ArrayList<>();
        for (Long lessonId : request.getLessonIds()) {
            lessons.add(getLessonInClassroomOrThrow(lessonId, classroomId));
        }

        timetableEntryRepository.delete()
                .eq(TimetableEntry::getClassroomId, classroomId)
                .eq(TimetableEntry::getDayOfWeek, dayOfWeek)
                .execute();

        LocalDateTime now = LocalDateTime.now();
        int orderIndex = 0;
        for (Lesson lesson : lessons) {
            TimetableEntry entry = new TimetableEntry();
            entry.setClassroomId(classroomId);
            entry.setDayOfWeek(dayOfWeek);
            entry.setLessonId(lesson.getId());
            entry.setOrderIndex(orderIndex++);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            entry.setCreatedBy("parent:" + parentId);
            entry.setUpdatedBy("parent:" + parentId);
            timetableEntryRepository.save(entry);
        }

        logInfo("Timetable day set: classroomId={}, dayOfWeek={}, lessonCount={}, parentId={}",
                classroomId, dayOfWeek, lessons.size(), parentId);
    }

    /**
     * Loads {@code lessonId}, throwing if it doesn't exist or its Subject does not belong to
     * {@code classroomId} - deliberately does NOT reuse {@code LessonService#getOwnedOrThrow}
     * (that only checks "belongs to the current Parent", i.e. ANY of their classrooms) because a
     * Lesson from the Parent's OTHER Classroom must still be rejected here (a Classroom's
     * timetable can only reference Lessons taught in that same Classroom).
     */
    private Lesson getLessonInClassroomOrThrow(Long lessonId, Long classroomId) {
        Lesson lesson = lessonRepository.findById(lessonId);
        if (lesson == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Lesson not found");
        }
        Subject subject = subjectRepository.findById(lesson.getSubjectId());
        if (subject == null || !subject.getClassroomId().equals(classroomId)) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                    "lessonId " + lessonId + " does not belong to a subject in this classroom");
        }
        return lesson;
    }
}
