package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.LessonImage;
import vn.org.thn.service.app.quiz.dto.StudentLessonResponse;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.Test;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.List;

/**
 * Student-facing read-only access to a Lesson's content (summary/content/textbookPage/image) -
 * task "Backend: Student xem lai noi dung bai hoc", 2026-09-01, so a Student can re-read the
 * lesson material both WHILE taking a Test built from it and AFTER submitting (both happen on
 * the same take-test screen in v1 - there is no separate post-submission results page, see {@code
 * StudentAttemptService}).
 * <p>
 * A Student may see a Lesson ONLY if it is reachable from at least one Test assigned to them:
 * {@code Lesson -> Question} (by {@code lessonId}) {@code -> TestQuestion} (by {@code
 * questionId}) {@code -> Test} (by {@code id}, filtered to {@code studentId}). There is no direct
 * Lesson<->Student relationship and MyBatis here has no real JOIN support (see the repository
 * classes' own javadocs across this package), so this is 3 sequential id-collecting queries
 * rather than 1 - the same shape {@code SubjectService#list} already uses to resolve
 * Classroom->Subject.
 * <p>
 * Every {@code .in()} call below is guarded against an empty id list first - {@code
 * BaseConditionBuilder#in} is a silent no-op on a null/empty collection (per its own javadoc), so
 * an unguarded call at any of these 3 hops would silently match EVERY row instead of none, which
 * here would leak every other student's lesson content. This is the exact bug class the Classroom
 * feature's {@code SubjectService#list} already had to guard against - see that class's javadoc.
 */
@Service
public class StudentLessonService extends IBase {

    @Autowired
    private LessonService lessonService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private TestRepository testRepository;

    public StudentLessonResponse get(Long lessonId) {
        Long studentId = CurrentUser.get().userId();
        Lesson lesson = lessonService.getById(lessonId);
        assertAccessible(lesson.getId(), studentId);
        return StudentLessonResponse.from(lesson);
    }

    public LessonImage getImage(Long lessonId) {
        Long studentId = CurrentUser.get().userId();
        Lesson lesson = lessonService.getById(lessonId);
        assertAccessible(lesson.getId(), studentId);
        return lessonService.loadImage(lesson);
    }

    /** Throws {@code COMMON_004 FORBIDDEN} unless some Test assigned to {@code studentId} was built from a Question belonging to {@code lessonId}. */
    private void assertAccessible(Long lessonId, Long studentId) {
        List<Long> questionIds = questionRepository.query().eq(Question::getLessonId, lessonId).list()
                .stream().map(Question::getId).toList();
        if (questionIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Lesson is not accessible");
        }

        List<Long> testIds = testQuestionRepository.query().in(TestQuestion::getQuestionId, questionIds).list()
                .stream().map(TestQuestion::getTestId).distinct().toList();
        if (testIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Lesson is not accessible");
        }

        boolean accessible = testRepository.query().in(Test::getId, testIds).eq(Test::getStudentId, studentId).exists();
        if (!accessible) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Lesson is not accessible");
        }
    }
}
