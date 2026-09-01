package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.QuestionResponse;
import vn.org.thn.service.app.quiz.dto.TestCreateRequest;
import vn.org.thn.service.app.quiz.dto.TestDetailResponse;
import vn.org.thn.service.app.quiz.dto.TestResponse;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.Test;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.entity.TestStatus;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test creation/listing/deletion for the currently logged-in Parent (task 5). Creating a Test
 * always assigns it to a Student in the same call - there is no separate "assign" step in v1
 * (see {@code docs/dev/05-tao-giao-bai-kiem-tra.md}).
 * <p>
 * {@code studentId} ownership reuses {@link StudentService#getOwnedOrThrow} and every element of
 * {@code questionIds} reuses {@link QuestionService#getOwnedOrThrow} (both package-private, same
 * reuse pattern as {@code LessonService}/{@code SubjectService}) - a Test itself does have its own
 * {@code parentId} column (unlike Lesson/Question), so this class's own {@code getOwnedOrThrow}
 * checks it directly rather than resolving through another entity.
 */
@Service
public class TestService extends IBase {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private QuestionService questionService;

    @Transactional
    public TestResponse create(TestCreateRequest request) {
        Long parentId = CurrentUser.get().userId();
        studentService.getOwnedOrThrow(request.getStudentId(), parentId);
        // Every questionId must belong to this parent too - checked one by one rather than a
        // single batch query, matching this codebase's existing ownership-check style (see
        // StudentService/SubjectService/LessonService); question lists are small (one Test's
        // worth), so the extra round trips are not a concern in v1.
        for (Long questionId : request.getQuestionIds()) {
            questionService.getOwnedOrThrow(questionId, parentId);
        }

        LocalDateTime now = LocalDateTime.now();
        Test test = new Test();
        test.setParentId(parentId);
        test.setStudentId(request.getStudentId());
        test.setName(request.getName());
        test.setStatus(TestStatus.ASSIGNED.name());
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        test.setCreatedBy("parent:" + parentId);
        test.setUpdatedBy("parent:" + parentId);
        test = testRepository.save(test);

        int orderIndex = 0;
        for (Long questionId : request.getQuestionIds()) {
            TestQuestion testQuestion = new TestQuestion();
            testQuestion.setTestId(test.getId());
            testQuestion.setQuestionId(questionId);
            testQuestion.setOrderIndex(orderIndex++);
            testQuestionRepository.save(testQuestion);
        }

        logInfo("Test created: id={}, studentId={}, parentId={}, questionCount={}",
                test.getId(), test.getStudentId(), parentId, request.getQuestionIds().size());
        return TestResponse.from(test);
    }

    /** Test detail with its questions in {@code orderIndex} order, each as the full Parent-facing {@link QuestionResponse} (including the correct choice). */
    public TestDetailResponse get(Long id) {
        Long parentId = CurrentUser.get().userId();
        Test test = getOwnedOrThrow(id, parentId);

        List<QuestionResponse> questions = testQuestionRepository.query()
                .eq(TestQuestion::getTestId, id)
                .list()
                .stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(testQuestion -> questionService.get(testQuestion.getQuestionId()))
                .toList();

        return TestDetailResponse.from(test, questions);
    }

    /** Every Test belonging to the current Parent, optionally narrowed to one Student - {@code studentId == null} means "every student" (see task 5 spec). */
    public List<TestResponse> list(Long studentId) {
        Long parentId = CurrentUser.get().userId();
        if (studentId != null) {
            studentService.getOwnedOrThrow(studentId, parentId);
        }
        // .eq() is a no-op when the value is null (see BaseConditionBuilder), so this one query
        // works unchanged whether studentId was supplied or not.
        return testRepository.query()
                .eq(Test::getParentId, parentId)
                .eq(Test::getStudentId, studentId)
                .list()
                .stream().map(TestResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Test test = getOwnedOrThrow(id, parentId);

        if (attemptRepository.query().eq(Attempt::getTestId, id).exists()) {
            throw new BusinessException(QuizErrorCode.TEST_HAS_ATTEMPTS);
        }
        testQuestionRepository.delete().eq(TestQuestion::getTestId, id).execute();
        testRepository.deleteById(test.getId());
        logInfo("Test deleted: id={}, parentId={}", test.getId(), parentId);
    }

    /** Loads the Test with id {@code id}, throwing if it doesn't exist or doesn't belong to {@code parentId}. Package-private so task 7's {@code ReportService} can reuse it. */
    Test getOwnedOrThrow(Long id, Long parentId) {
        Test test = testRepository.findById(id);
        if (test == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Test not found");
        }
        if (!test.getParentId().equals(parentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This test does not belong to the current parent");
        }
        return test;
    }
}
