package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.LessonReportCandidate;
import vn.org.thn.service.app.quiz.dto.LessonReportHistoryItem;
import vn.org.thn.service.app.quiz.dto.SubjectLessonReportStatus;
import vn.org.thn.service.app.quiz.dto.TimetableEntryResponse;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.LessonReport;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.LessonReportRepository;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Bao bai" - a Student confirming they studied one specific {@link Lesson} on a real date
 * (2026-09-06, "hom nay con hoc gi": "hom nay con hoc toan -> con hoc bai 1", because a Subject
 * like Toan can have "Bai 1".."Bai 100" and the Timetable itself no longer pins which one - see
 * {@code TimetableEntry}'s 2026-09-06 revision javadoc). Unlike {@link LessonPreparationService}
 * (forward-looking, Subject-level, toggle-only-for-tomorrow) this is backward-looking and
 * Lesson-level: every report is a permanent history row once its day has passed.
 * <p>
 * Design decisions (AskUserQuestion 2026-09-06): a Lesson may only be reported if its Subject is
 * on TODAY's timetable; multiple Lessons per Subject per day are allowed; only today's own
 * reports can be undone; a Lesson reported on any date is hidden from the picker forever
 * (enforced by the DB's unique (studentId, lessonId) constraint - see {@link LessonReport}'s
 * javadoc).
 */
@Service
public class LessonReportService extends IBase {

    @Autowired
    private LessonReportRepository lessonReportRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TimetableService timetableService;

    // ---- Student-facing (self) ----

    /** Today's timetable Subjects for the current Student, each with what has already been reported today and what is still pickable. */
    public List<SubjectLessonReportStatus> getMyTodayStatus() {
        Long studentId = CurrentUser.get().userId();
        Student student = getStudentOrThrow(studentId);
        return buildTodayStatus(student, studentId);
    }

    /**
     * Reports {@code lessonId} as studied today. Rejects a Lesson whose Subject is not on
     * today's timetable ({@link QuizErrorCode#LESSON_REPORT_SUBJECT_NOT_TODAY}), and a Lesson
     * already reported on ANY date, including today ({@link QuizErrorCode#LESSON_ALREADY_REPORTED}
     * - unlike {@code LessonPreparationService#markPrepared}, this is deliberately NOT idempotent,
     * since a second report of the same Lesson would violate the "hidden forever once reported"
     * rule the frontend's own filtering already relies on).
     */
    @Transactional
    public List<SubjectLessonReportStatus> reportLesson(Long lessonId) {
        Long studentId = CurrentUser.get().userId();
        Student student = getStudentOrThrow(studentId);
        Lesson lesson = getLessonInClassroomOrThrow(lessonId, student.getClassroomId());

        LocalDate today = LocalDate.now();
        boolean subjectScheduledToday = timetableService.getForClassroomAndDate(student.getClassroomId(), today)
                .stream().anyMatch(entry -> entry.getSubjectId().equals(lesson.getSubjectId()));
        if (!subjectScheduledToday) {
            throw new BusinessException(QuizErrorCode.LESSON_REPORT_SUBJECT_NOT_TODAY);
        }

        boolean alreadyReported = lessonReportRepository.query()
                .eq(LessonReport::getStudentId, studentId)
                .eq(LessonReport::getLessonId, lessonId)
                .exists();
        if (alreadyReported) {
            throw new BusinessException(QuizErrorCode.LESSON_ALREADY_REPORTED);
        }

        LessonReport row = new LessonReport();
        row.setStudentId(studentId);
        row.setLessonId(lessonId);
        row.setReportDate(today);
        LocalDateTime now = LocalDateTime.now();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setCreatedBy("student:" + studentId);
        row.setUpdatedBy("student:" + studentId);
        lessonReportRepository.save(row);

        logInfo("Lesson reported: studentId={}, lessonId={}, date={}", studentId, lessonId, today);
        return buildTodayStatus(student, studentId);
    }

    /**
     * Undoes a report - only when it was made TODAY ({@link QuizErrorCode#LESSON_REPORT_LOCKED}
     * otherwise), so history from a previous day can never be edited away. Idempotent if
     * {@code lessonId} was never reported at all (no-op, not an error), same as {@code
     * LessonPreparationService#unmarkPrepared}.
     */
    @Transactional
    public List<SubjectLessonReportStatus> unreportLesson(Long lessonId) {
        Long studentId = CurrentUser.get().userId();
        Student student = getStudentOrThrow(studentId);

        LessonReport row = lessonReportRepository.query()
                .eq(LessonReport::getStudentId, studentId)
                .eq(LessonReport::getLessonId, lessonId)
                .one();
        if (row != null) {
            if (!row.getReportDate().isEqual(LocalDate.now())) {
                throw new BusinessException(QuizErrorCode.LESSON_REPORT_LOCKED);
            }
            lessonReportRepository.delete().eq(LessonReport::getId, row.getId()).execute();
            logInfo("Lesson report undone: studentId={}, lessonId={}", studentId, lessonId);
        }
        return buildTodayStatus(student, studentId);
    }

    private List<SubjectLessonReportStatus> buildTodayStatus(Student student, Long studentId) {
        LocalDate today = LocalDate.now();
        List<TimetableEntryResponse> todaySubjects = timetableService.getForClassroomAndDate(student.getClassroomId(), today);

        List<LessonReport> myReports = lessonReportRepository.query()
                .eq(LessonReport::getStudentId, studentId)
                .list();
        Set<Long> everReportedLessonIds = myReports.stream().map(LessonReport::getLessonId).collect(Collectors.toSet());
        Set<Long> todayReportedLessonIds = myReports.stream()
                .filter(r -> r.getReportDate().isEqual(today))
                .map(LessonReport::getLessonId).collect(Collectors.toSet());

        return todaySubjects.stream().map(entry -> {
            List<Lesson> lessons = lessonRepository.query()
                    .eq(Lesson::getSubjectId, entry.getSubjectId()).list();
            lessons.sort(Comparator.comparing(Lesson::getId));

            List<LessonReportCandidate> reportedToday = lessons.stream()
                    .filter(l -> todayReportedLessonIds.contains(l.getId()))
                    .map(l -> new LessonReportCandidate(l.getId(), l.getName()))
                    .toList();
            List<LessonReportCandidate> available = lessons.stream()
                    .filter(l -> !everReportedLessonIds.contains(l.getId()))
                    .map(l -> new LessonReportCandidate(l.getId(), l.getName()))
                    .toList();

            return new SubjectLessonReportStatus(entry.getSubjectId(), entry.getSubjectName(), entry.getOrderIndex(), reportedToday, available);
        }).toList();
    }

    // ---- Parent-facing (read-only, any owned Student) ----

    /**
     * One date's reported Lessons for {@code studentId}, newest-subject-first is not guaranteed -
     * sorted by Subject name then Lesson id for stable, predictable display. {@code date}
     * defaults to today when null (item request: "mac dinh lay ngay hom nay"); {@code
     * subjectIdFilter} is optional (null = every Subject).
     */
    public List<LessonReportHistoryItem> getStudentHistory(Long studentId, LocalDate date, Long subjectIdFilter) {
        Long parentId = CurrentUser.get().userId();
        Student student = studentService.getOwnedOrThrow(studentId, parentId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;

        List<LessonReport> rows = lessonReportRepository.query()
                .eq(LessonReport::getStudentId, studentId)
                .eq(LessonReport::getReportDate, targetDate)
                .list();

        return rows.stream()
                .map(row -> {
                    Lesson lesson = lessonRepository.findById(row.getLessonId());
                    Subject subject = lesson == null ? null : subjectRepository.findById(lesson.getSubjectId());
                    return new LessonReportHistoryItem(
                            subject == null ? null : subject.getId(),
                            subject == null ? null : subject.getName(),
                            lesson == null ? null : lesson.getId(),
                            lesson == null ? null : lesson.getName(),
                            row.getReportDate());
                })
                .filter(item -> subjectIdFilter == null || subjectIdFilter.equals(item.getSubjectId()))
                .sorted(Comparator.comparing((LessonReportHistoryItem i) -> i.getSubjectName() == null ? "" : i.getSubjectName())
                        .thenComparing(i -> i.getLessonId() == null ? Long.MIN_VALUE : i.getLessonId()))
                .toList();
    }

    private Student getStudentOrThrow(Long studentId) {
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }
        return student;
    }

    /**
     * Loads {@code lessonId}, throwing if it doesn't exist or its Subject does not belong to
     * {@code classroomId} - same shape as {@code TimetableService}'s own (now-removed)
     * subject-in-classroom check, just one level deeper (Lesson -> Subject -> Classroom) since
     * this feature needs actual Lesson identity, not just Subject.
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
