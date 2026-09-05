package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.LessonPreparationStatus;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.entity.LessonPreparation;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.repository.LessonPreparationRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Prepared for tomorrow" checklist - items 9 and 10 of the 2026-09-05 batch request. Item 9:
 * "hoc sinh chuan bi bai cho ngay mai theo thoi khoa bieu bang cach danh dau da chuan bi bai va
 * gui cho phu huynh" - the Student ticks off each of tomorrow's {@link
 * vn.org.thn.service.app.quiz.entity.TimetableEntry} subjects as prepared; "gui cho phu huynh"
 * needs no explicit send/notify step here, since the Parent simply reads the same {@link
 * LessonPreparation} rows this class writes (item 10: "phu huynh dua vao ket qua cua 9 de biet
 * con da chuan bi bai cho ngay mai hay chua va mon nao chua hoc").
 * <p>
 * <b>Revision 2026-09-06:</b> tracks {@code subjectId} now, not {@code lessonId} - see {@link
 * LessonPreparation}'s javadoc for why (the timetable itself became Subject-level only).
 * <p>
 * Always resolves "tomorrow" as {@code LocalDate.now().plusDays(1)} at call time - deliberately
 * NOT a caller-supplied date parameter anywhere in this class's public API, since both items 9
 * and 10 are explicitly about "ngay mai" (tomorrow) and nothing else; a generic by-date API was
 * considered and rejected as unneeded scope for this feature.
 * <p>
 * Reuses {@link TimetableService#getForClassroomAndDate} for the "what is scheduled" half of the
 * status list (same method backing {@code StudentTimetableService}'s own today/tomorrow view),
 * and merges in whether a {@link LessonPreparation} row exists for each subject.
 */
@Service
public class LessonPreparationService extends IBase {

    @Autowired
    private LessonPreparationRepository lessonPreparationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private StudentService studentService;

    // ---- Student-facing (self) - item 9 ----

    /** Tomorrow's timetable for the current Student, each subject flagged whether already marked prepared. */
    public List<LessonPreparationStatus> getMyTomorrowStatus() {
        Long studentId = CurrentUser.get().userId();
        Student student = getStudentOrThrow(studentId);
        return buildStatus(student.getClassroomId(), studentId, tomorrow());
    }

    /**
     * Marks {@code subjectId} as prepared for tomorrow - idempotent (marking an already-prepared
     * subject again is a no-op, not an error, so the frontend checkbox never needs to know
     * whether it is already checked before calling this). Rejects a {@code subjectId} that is not
     * actually on tomorrow's timetable for this Student's classroom (COMMON_002) - otherwise a
     * Student could mark an arbitrary subjectId "prepared", and item 10's Parent view would show a
     * phantom entry with no corresponding timetable slot.
     */
    @Transactional
    public void markPrepared(Long subjectId) {
        Long studentId = CurrentUser.get().userId();
        Student student = getStudentOrThrow(studentId);
        LocalDate date = tomorrow();

        boolean scheduledTomorrow = timetableService.getForClassroomAndDate(student.getClassroomId(), date)
                .stream().anyMatch(entry -> entry.getSubjectId().equals(subjectId));
        if (!scheduledTomorrow) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "subjectId is not in tomorrow's timetable for this student");
        }

        boolean alreadyMarked = lessonPreparationRepository.query()
                .eq(LessonPreparation::getStudentId, studentId)
                .eq(LessonPreparation::getTargetDate, date)
                .eq(LessonPreparation::getSubjectId, subjectId)
                .one() != null;
        if (!alreadyMarked) {
            LessonPreparation row = new LessonPreparation();
            row.setStudentId(studentId);
            row.setTargetDate(date);
            row.setSubjectId(subjectId);
            LocalDateTime now = LocalDateTime.now();
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            row.setCreatedBy("student:" + studentId);
            row.setUpdatedBy("student:" + studentId);
            lessonPreparationRepository.save(row);
        }
        logInfo("Lesson preparation marked: studentId={}, date={}, subjectId={}", studentId, date, subjectId);
    }

    /** Un-marks {@code subjectId} for tomorrow - idempotent, no error if it was not marked. */
    @Transactional
    public void unmarkPrepared(Long subjectId) {
        Long studentId = CurrentUser.get().userId();
        LocalDate date = tomorrow();
        lessonPreparationRepository.delete()
                .eq(LessonPreparation::getStudentId, studentId)
                .eq(LessonPreparation::getTargetDate, date)
                .eq(LessonPreparation::getSubjectId, subjectId)
                .execute();
        logInfo("Lesson preparation unmarked: studentId={}, date={}, subjectId={}", studentId, date, subjectId);
    }

    // ---- Parent-facing (read-only, any owned Student) - item 10 ----

    /** Same shape as {@link #getMyTomorrowStatus} but for one of the current Parent's students - read-only, the Parent can never mark/unmark on the Student's behalf. */
    public List<LessonPreparationStatus> getStudentTomorrowStatus(Long studentId) {
        Long parentId = CurrentUser.get().userId();
        Student student = studentService.getOwnedOrThrow(studentId, parentId);
        return buildStatus(student.getClassroomId(), studentId, tomorrow());
    }

    private List<LessonPreparationStatus> buildStatus(Long classroomId, Long studentId, LocalDate date) {
        List<TimetableEntryResponse> entries = timetableService.getForClassroomAndDate(classroomId, date);
        Set<Long> preparedSubjectIds = lessonPreparationRepository.query()
                .eq(LessonPreparation::getStudentId, studentId)
                .eq(LessonPreparation::getTargetDate, date)
                .list().stream().map(LessonPreparation::getSubjectId).collect(Collectors.toSet());
        return entries.stream()
                .map(entry -> new LessonPreparationStatus(
                        entry.getSubjectId(),
                        entry.getSubjectName(),
                        entry.getOrderIndex(),
                        preparedSubjectIds.contains(entry.getSubjectId())))
                .toList();
    }

    private LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    private Student getStudentOrThrow(Long studentId) {
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }
        return student;
    }
}
