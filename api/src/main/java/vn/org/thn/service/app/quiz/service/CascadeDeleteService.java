package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.LessonPreparation;
import vn.org.thn.service.app.quiz.entity.LessonReport;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.SubjectLibraryLink;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.entity.TimetableEntry;
import vn.org.thn.service.app.quiz.repository.AttemptAnswerRepository;
import vn.org.thn.service.app.quiz.repository.AttemptRepository;
import vn.org.thn.service.app.quiz.repository.ChoiceRepository;
import vn.org.thn.service.app.quiz.repository.LessonPreparationRepository;
import vn.org.thn.service.app.quiz.repository.LessonReportRepository;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.SubjectLibraryLinkRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.repository.TimetableEntryRepository;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.db.DatabasePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Cascading delete for Subject/Lesson/Question/Test (2026-09-06, per the user's explicit
 * request: "phu huynh can duoc xoa cac du lieu nhu mon hoc, bai hoc, de on, neu cac du lieu do
 * duoc lien ket voi hoc sinh thi xoa luon nhung du lieu lien quan" - "the Parent needs to be able
 * to delete Subject/Lesson/Test data, and if that data is linked to a Student, delete the linked
 * data too"). Before this revision, {@code SubjectService#delete}/{@code LessonService#delete}/
 * {@code QuestionService#delete}/{@code TestService#delete} all BLOCKED outright the moment any
 * child/linked row existed ({@code SUBJECT_HAS_LESSONS}/{@code LESSON_HAS_QUESTIONS}/{@code
 * QUESTION_USED_IN_TEST}/{@code TEST_HAS_ATTEMPTS}) - this class replaces those hard blocks with
 * an actual cascade, confirmed via AskUserQuestion (2026-09-06):
 * <ol>
 *     <li>A Test that already has Attempts CAN now be deleted - its Attempts and their
 *     AttemptAnswer rows (and any recorded speaking-answer audio files) are deleted with it. This
 *     permanently loses that Test's score history - deliberate, per the user's explicit choice
 *     ("Cho xoa luon, mat lich su diem") over keeping the old hard block.</li>
 *     <li>Deleting a Subject/Lesson/Question that has a Question used in ANY Test deletes that
 *     WHOLE Test too (rule 1 above applies to it), even if the Test also has unrelated questions
 *     from a different Subject/Lesson - per the user's explicit choice ("Xoa luon ca De kiem tra
 *     chua cau hoi do") over only stripping the one question out of the Test. Applied uniformly
 *     to a direct single-Question delete too, for consistency with the Subject/Lesson case -
 *     this specific extension was not itself asked about, flagged here for review.</li>
 * </ol>
 * <p>
 * Each {@code deleteXxxCascade} method deletes every row that depends on the given id(s) AND the
 * id(s) themselves - callers ({@code SubjectService#delete} etc.) still do their own ownership
 * check first (this class trusts the id(s) it is given completely, same "caller already proved
 * ownership" convention as every other package-private cross-class reuse in this codebase), then
 * call straight in here instead of doing the deletion themselves.
 * <p>
 * <b>Why this class only depends on repositories, never on {@code SubjectService}/{@code
 * LessonService}/{@code QuestionService}/{@code TestService} themselves:</b> those 4 services
 * already depend on EACH OTHER in one direction only (Question -&gt; Lesson -&gt; Subject,
 * Test -&gt; Question/Lesson/Subject, matching the "child resolves ownership through its parent"
 * convention used throughout this codebase). Cascading delete inherently needs to walk the SAME
 * graph in reverse (Subject needs to reach down into Test), so if this class called back into
 * those services' own methods, the 4 services and this class would form a circular Spring bean
 * dependency. Repositories have no such direction (they are leaf beans), so autowiring them
 * directly here - including the 3 upload-directory constants below, deliberately duplicated from
 * {@code LessonService}/{@code QuestionService}/{@code StudentAttemptService} rather than reusing
 * their own private file-cleanup helpers - keeps this class dependency-free and avoids the cycle
 * entirely, at the small cost of 4 duplicated {@code Path} constants.
 */
@Service
public class CascadeDeleteService extends IBase {

    /** Same path as {@code LessonService#IMAGE_DIR} - see this class's javadoc for why it is duplicated here instead of reused. */
    private static final Path LESSON_IMAGE_DIR = DatabasePath.HOME.resolve("uploads").resolve("lessons");
    /** Same path as {@code QuestionService#AUDIO_DIR}. */
    private static final Path QUESTION_AUDIO_DIR = DatabasePath.HOME.resolve("uploads").resolve("questions");
    /** Same path as {@code QuestionService#VIDEO_DIR}. */
    private static final Path QUESTION_VIDEO_DIR = DatabasePath.HOME.resolve("uploads").resolve("question-videos");
    /** Same path as {@code StudentAttemptService#SPEAKING_ANSWER_DIR}. */
    private static final Path SPEAKING_ANSWER_DIR = DatabasePath.HOME.resolve("uploads").resolve("speaking-answers");

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonReportRepository lessonReportRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private LessonPreparationRepository lessonPreparationRepository;

    @Autowired
    private SubjectLibraryLinkRepository subjectLibraryLinkRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    /**
     * Deletes every {@code testIds} Test, including its Attempts, their AttemptAnswer rows (and
     * any recorded speaking-answer audio files), and its TestQuestion rows. No-op on an empty
     * list (never call {@code .in()} with an empty collection - see this codebase's own
     * established warning on that, a silent no-op would read as "no filter" instead of "no
     * matches").
     */
    @Transactional
    public void deleteTestsCascade(List<Long> testIds) {
        if (testIds.isEmpty()) {
            return;
        }
        List<Long> attemptIds = attemptRepository.query().in(Attempt::getTestId, testIds).list()
                .stream().map(Attempt::getId).toList();
        if (!attemptIds.isEmpty()) {
            List<AttemptAnswer> answers = attemptAnswerRepository.query().in(AttemptAnswer::getAttemptId, attemptIds).list();
            for (AttemptAnswer answer : answers) {
                deleteFileQuietly(SPEAKING_ANSWER_DIR, answer.getAnswerAudioPath());
            }
            attemptAnswerRepository.delete().in(AttemptAnswer::getAttemptId, attemptIds).execute();
            attemptRepository.delete().in(Attempt::getTestId, testIds).execute();
        }
        testQuestionRepository.delete().in(TestQuestion::getTestId, testIds).execute();
        for (Long testId : testIds) {
            testRepository.deleteById(testId);
        }
        logInfo("Cascade-deleted {} test(s) (with any attempts/answers): {}", testIds.size(), testIds);
    }

    /**
     * Deletes every {@code questionIds} Question, first cascading away (via {@link
     * #deleteTestsCascade}) every Test that has ANY of these questions on it - see class javadoc,
     * decision 2. Then deletes each question's Choices, audio/video files, and its own row.
     */
    @Transactional
    public void deleteQuestionsCascade(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return;
        }
        List<Long> testIds = testQuestionRepository.query().in(TestQuestion::getQuestionId, questionIds).list()
                .stream().map(TestQuestion::getTestId).distinct().toList();
        deleteTestsCascade(testIds);

        List<Question> questions = questionRepository.query().in(Question::getId, questionIds).list();
        choiceRepository.delete().in(Choice::getQuestionId, questionIds).execute();
        for (Question question : questions) {
            deleteFileQuietly(QUESTION_AUDIO_DIR, question.getAudioPath());
            deleteFileQuietly(QUESTION_VIDEO_DIR, question.getVideoPath());
        }
        for (Long questionId : questionIds) {
            questionRepository.deleteById(questionId);
        }
        logInfo("Cascade-deleted {} question(s), affecting {} test(s)", questionIds.size(), testIds.size());
    }

    /**
     * Deletes every {@code lessonIds} Lesson, first cascading away every Question under them (via
     * {@link #deleteQuestionsCascade}), then every {@code LessonReport} row for them (2026-09-06
     * "Bao bai" history - permanently lost once its Lesson is gone, same "linked to a student ->
     * delete it too" rule as everything else in this class), then each lesson's image file and
     * its own row.
     */
    @Transactional
    public void deleteLessonsCascade(List<Long> lessonIds) {
        if (lessonIds.isEmpty()) {
            return;
        }
        List<Long> questionIds = questionRepository.query().in(Question::getLessonId, lessonIds).list()
                .stream().map(Question::getId).toList();
        deleteQuestionsCascade(questionIds);

        lessonReportRepository.delete().in(LessonReport::getLessonId, lessonIds).execute();

        List<Lesson> lessons = lessonRepository.query().in(Lesson::getId, lessonIds).list();
        for (Lesson lesson : lessons) {
            deleteFileQuietly(LESSON_IMAGE_DIR, lesson.getImagePath());
        }
        for (Long lessonId : lessonIds) {
            lessonRepository.deleteById(lessonId);
        }
        logInfo("Cascade-deleted {} lesson(s), {} question(s) under them", lessonIds.size(), questionIds.size());
    }

    /**
     * Deletes {@code subjectId} entirely: every Lesson under it (via {@link
     * #deleteLessonsCascade}), every {@code TimetableEntry} slot naming it (any classroom/day),
     * every {@code LessonPreparation} ("chuan bi bai") row for it (any date), every {@code
     * SubjectLibraryLink} to it (only the link row - the linked {@code LibraryDocument} itself is
     * Admin-owned content, never touched), and finally the Subject row itself.
     */
    @Transactional
    public void deleteSubjectCascade(Long subjectId) {
        List<Long> lessonIds = lessonRepository.query().eq(Lesson::getSubjectId, subjectId).list()
                .stream().map(Lesson::getId).toList();
        deleteLessonsCascade(lessonIds);

        timetableEntryRepository.delete().eq(TimetableEntry::getSubjectId, subjectId).execute();
        lessonPreparationRepository.delete().eq(LessonPreparation::getSubjectId, subjectId).execute();
        subjectLibraryLinkRepository.delete().eq(SubjectLibraryLink::getSubjectId, subjectId).execute();
        subjectRepository.deleteById(subjectId);
        logInfo("Cascade-deleted subject id={}, {} lesson(s) under it", subjectId, lessonIds.size());
    }

    /**
     * Deletes every {@link TimetableEntry} and {@link LessonPreparation} row belonging to {@code
     * studentId} (2026-09-06, revision (b) of {@code TimetableEntry} - see its javadoc). Added
     * specifically because {@code timetable_entry} just gained a {@code student_id NOT NULL
     * REFERENCES student(id)} foreign key (V7 migration) - without this cleanup, {@code
     * StudentService#delete} would start throwing a live foreign-key-constraint-violation error
     * the moment a Student with any timetable entry was deleted. {@code lesson_preparation} was
     * ALREADY student-scoped before this revision and had the exact same pre-existing gap
     * (discovered during this same investigation - it was never cleaned up on Student delete
     * either), so it is fixed here too, in the same pass, for the same reason.
     * <p>
     * Deliberately does NOT also cover {@code test}/{@code attempt}/{@code lesson_report}, which
     * this same investigation found ALSO have a live {@code REFERENCES student(id)} foreign key
     * with zero cascade-on-student-delete cleanup today (a pre-existing bug, not introduced by
     * this change) - deleting Test/Attempt history is a materially bigger, more destructive
     * decision (permanent loss of grading history) than this method's two tables, and was not
     * part of what the user asked for in this request; flagged for the user rather than silently
     * folded in here.
     */
    @Transactional
    public void deleteStudentTimetableDataCascade(Long studentId) {
        timetableEntryRepository.delete().eq(TimetableEntry::getStudentId, studentId).execute();
        lessonPreparationRepository.delete().eq(LessonPreparation::getStudentId, studentId).execute();
        logInfo("Cascade-deleted timetable/lesson-preparation data for studentId={}", studentId);
    }

    /** Best-effort delete - a missing/already-gone file is not an error worth failing the caller's request over, same reasoning as every other {@code deleteXxxFileQuietly} in this codebase. */
    private void deleteFileQuietly(Path dir, String filename) {
        if (filename == null) {
            return;
        }
        try {
            Files.deleteIfExists(dir.resolve(filename));
        } catch (IOException e) {
            log().warn("Could not delete file {} in {}: {}", filename, dir, e.getMessage());
        }
    }
}
