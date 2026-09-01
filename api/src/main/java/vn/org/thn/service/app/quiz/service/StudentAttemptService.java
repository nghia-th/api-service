package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.AnswerItem;
import vn.org.thn.service.app.quiz.dto.AnswerRequest;
import vn.org.thn.service.app.quiz.dto.QuestionAudio;
import vn.org.thn.service.app.quiz.dto.StartAttemptResponse;
import vn.org.thn.service.app.quiz.dto.StudentPracticeGenerateRequest;
import vn.org.thn.service.app.quiz.dto.StudentQuestionResponse;
import vn.org.thn.service.app.quiz.dto.StudentTestSummaryResponse;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
import vn.org.thn.service.app.quiz.dto.SubmitAttemptResponse;
import vn.org.thn.service.app.quiz.dto.TestResponse;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.entity.Test;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.entity.TestStatus;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptAnswerRepository;
import vn.org.thn.service.app.quiz.repository.AttemptRepository;
import vn.org.thn.service.app.quiz.repository.ChoiceRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Student-facing side of task 6 ({@code /api/student/**}) - listing assigned tests, starting/
 * resuming an attempt, saving answers, and submitting for grading. Every method reads {@link
 * CurrentUser#get()} itself, same pattern as every Parent-facing service, but here {@code
 * userId()} is a Student id (the {@link vn.org.thn.service.app.quiz.security.JwtAuthFilter}
 * already guarantees the token's role is STUDENT for anything under this prefix).
 * <p>
 * v1 allows at most 1 {@link Attempt} per {@link Test} - {@link #start} is idempotent (returns the
 * existing Attempt instead of creating a second one), per the task 6 spec.
 * <p>
 * {@code listSubjects}/{@code generatePractice} (added 2026-09-01, "On tap kien thuc") let the
 * Student self-generate a practice Test without any Parent involvement - the actual random-pick
 * logic lives in {@link TestService#generatePracticeForStudent} (this class just resolves the
 * current Student id and delegates), same "thin controller-ish service delegates to the entity's
 * owning service" shape {@link StudentLessonService} already uses for viewing Lesson content.
 */
@Service
public class StudentAttemptService extends IBase {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TestService testService;

    @Autowired
    private QuestionService questionService;

    /** Every Test assigned to the current Student, with status so they know what is done vs. pending. */
    public List<StudentTestSummaryResponse> listTests() {
        Long studentId = CurrentUser.get().userId();
        return testRepository.query().eq(Test::getStudentId, studentId).list().stream()
                .map(StudentTestSummaryResponse::from).toList();
    }

    /** Every Subject in the current Student's own Classroom - populates the "chọn Môn" dropdown for {@link #generatePractice}, the Student never picks a Classroom (they only ever have the one). */
    public List<SubjectResponse> listSubjects() {
        Long studentId = CurrentUser.get().userId();
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }
        return subjectRepository.query().eq(Subject::getClassroomId, student.getClassroomId()).list().stream()
                .map(SubjectResponse::from).toList();
    }

    /** Self-service "On tap kien thuc" generation - {@code POST /api/student/tests/practice}. Delegates the actual random-pick + Test/TestQuestion creation to {@link TestService#generatePracticeForStudent}, passing only the current Student's own id (never trusts a studentId from the request body - there is none on {@link StudentPracticeGenerateRequest}, by design). */
    public TestResponse generatePractice(StudentPracticeGenerateRequest request) {
        Long studentId = CurrentUser.get().userId();
        return testService.generatePracticeForStudent(studentId, request.getSubjectId(), request.getName(), request.getQuestionCount());
    }

    /**
     * Student-facing audio playback for a listening question (task "Cau hoi dang am thanh",
     * 2026-09-01) - {@code GET /api/student/questions/{id}/audio}. Access is per-question, not
     * blanket: only reachable if some Test assigned to the current Student has a {@code
     * TestQuestion} pointing at this questionId - a direct, 1-hop version of {@code
     * StudentLessonService#assertAccessible}'s multi-hop check (no {@code Lesson} involved here,
     * {@code Question} is already directly on {@code TestQuestion}). Reuses {@link
     * QuestionService#getById}/{@link QuestionService#loadAudio} (both package-private, same reuse
     * shape {@code StudentLessonService} already uses on {@code LessonService}) rather than
     * duplicating file-reading logic here.
     */
    public QuestionAudio getQuestionAudio(Long questionId) {
        Long studentId = CurrentUser.get().userId();
        // Throws NOT_FOUND itself if the question does not exist at all - checked before the
        // access check below so a bad id never leaks a FORBIDDEN vs NOT_FOUND distinction either
        // way (same shape as every other getOwnedOrThrow-style lookup in this codebase).
        Question question = questionService.getById(questionId);

        List<Long> testIds = testQuestionRepository.query().eq(TestQuestion::getQuestionId, questionId).list()
                .stream().map(TestQuestion::getTestId).distinct().toList();
        boolean accessible = !testIds.isEmpty()
                && testRepository.query().in(Test::getId, testIds).eq(Test::getStudentId, studentId).exists();
        if (!accessible) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Question audio is not accessible");
        }

        return questionService.loadAudio(question);
    }

    @Transactional
    public StartAttemptResponse start(Long testId) {
        Long studentId = CurrentUser.get().userId();
        getOwnedTestOrThrow(testId, studentId);

        Attempt attempt = attemptRepository.query().eq(Attempt::getTestId, testId).one();
        if (attempt == null) {
            List<TestQuestion> testQuestions = testQuestionRepository.query().eq(TestQuestion::getTestId, testId).list();
            LocalDateTime now = LocalDateTime.now();
            attempt = new Attempt();
            attempt.setTestId(testId);
            attempt.setStudentId(studentId);
            attempt.setStartedAt(now);
            attempt.setTotalQuestions(testQuestions.size());
            attempt.setCreatedAt(now);
            attempt.setUpdatedAt(now);
            attempt.setCreatedBy("student:" + studentId);
            attempt.setUpdatedBy("student:" + studentId);
            attempt = attemptRepository.save(attempt);
            logInfo("Attempt started: id={}, testId={}, studentId={}", attempt.getId(), testId, studentId);
        }

        return new StartAttemptResponse(attempt.getId(), studentQuestionsOf(testId));
    }

    @Transactional
    public void saveAnswers(Long attemptId, AnswerRequest request) {
        Long studentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedAttemptOrThrow(attemptId, studentId);
        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }

        Set<Long> testQuestionIds = testQuestionRepository.query().eq(TestQuestion::getTestId, attempt.getTestId())
                .list().stream().map(TestQuestion::getQuestionId).collect(Collectors.toSet());

        for (AnswerItem item : request.getAnswers()) {
            if (!testQuestionIds.contains(item.getQuestionId())) {
                throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                        "questionId " + item.getQuestionId() + " is not part of this attempt's test");
            }
            Choice choice = choiceRepository.findById(item.getChoiceId());
            if (choice == null || !choice.getQuestionId().equals(item.getQuestionId())) {
                throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                        "choiceId " + item.getChoiceId() + " does not belong to questionId " + item.getQuestionId());
            }

            AttemptAnswer answer = attemptAnswerRepository.query()
                    .eq(AttemptAnswer::getAttemptId, attemptId)
                    .eq(AttemptAnswer::getQuestionId, item.getQuestionId())
                    .one();
            if (answer == null) {
                answer = new AttemptAnswer();
                answer.setAttemptId(attemptId);
                answer.setQuestionId(item.getQuestionId());
            }
            // correct is intentionally left as-is (null until submit) - grading only happens once,
            // at submit time, see #submit.
            answer.setChoiceId(item.getChoiceId());
            attemptAnswerRepository.save(answer);
        }
    }

    @Transactional
    public SubmitAttemptResponse submit(Long attemptId) {
        Long studentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedAttemptOrThrow(attemptId, studentId);
        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }

        List<TestQuestion> testQuestions = testQuestionRepository.query().eq(TestQuestion::getTestId, attempt.getTestId()).list();
        Map<Long, AttemptAnswer> answerByQuestionId = new HashMap<>();
        for (AttemptAnswer answer : attemptAnswerRepository.query().eq(AttemptAnswer::getAttemptId, attemptId).list()) {
            answerByQuestionId.put(answer.getQuestionId(), answer);
        }

        int correctCount = 0;
        for (TestQuestion testQuestion : testQuestions) {
            Long questionId = testQuestion.getQuestionId();
            AttemptAnswer answer = answerByQuestionId.get(questionId);
            if (answer == null) {
                // Never answered at all - still recorded as a blank/wrong AttemptAnswer row so
                // task 7's report has one row per question, per the task 6 spec.
                answer = new AttemptAnswer();
                answer.setAttemptId(attemptId);
                answer.setQuestionId(questionId);
            }
            boolean isCorrect = false;
            if (answer.getChoiceId() != null) {
                Choice chosen = choiceRepository.findById(answer.getChoiceId());
                isCorrect = chosen != null && Boolean.TRUE.equals(chosen.getCorrect());
            }
            answer.setCorrect(isCorrect);
            attemptAnswerRepository.save(answer);
            if (isCorrect) {
                correctCount++;
            }
        }

        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setCorrectCount(correctCount);
        attempt.setUpdatedAt(LocalDateTime.now());
        attempt.setUpdatedBy("student:" + studentId);
        attempt = attemptRepository.save(attempt);

        Test test = testRepository.findById(attempt.getTestId());
        test.setStatus(TestStatus.COMPLETED.name());
        test.setUpdatedAt(LocalDateTime.now());
        test.setUpdatedBy("student:" + studentId);
        testRepository.save(test);

        logInfo("Attempt submitted: id={}, testId={}, studentId={}, correctCount={}, totalQuestions={}",
                attempt.getId(), attempt.getTestId(), studentId, correctCount, attempt.getTotalQuestions());
        return SubmitAttemptResponse.from(attempt);
    }

    /** Loads the Test with id {@code testId}, throwing if it doesn't exist or isn't assigned to {@code studentId}. */
    private Test getOwnedTestOrThrow(Long testId, Long studentId) {
        Test test = testRepository.findById(testId);
        if (test == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Test not found");
        }
        if (!test.getStudentId().equals(studentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This test is not assigned to the current student");
        }
        return test;
    }

    /** Loads the Attempt with id {@code attemptId}, throwing if it doesn't exist or doesn't belong to {@code studentId}. */
    private Attempt getOwnedAttemptOrThrow(Long attemptId, Long studentId) {
        Attempt attempt = attemptRepository.findById(attemptId);
        if (attempt == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Attempt not found");
        }
        if (!attempt.getStudentId().equals(studentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This attempt does not belong to the current student");
        }
        return attempt;
    }

    /** The test's questions in display order, mapped to the no-correct-answer Student view - see {@code StudentChoiceResponse}. */
    private List<StudentQuestionResponse> studentQuestionsOf(Long testId) {
        List<TestQuestion> testQuestions = testQuestionRepository.query().eq(TestQuestion::getTestId, testId).list();
        testQuestions.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));

        return testQuestions.stream().map(testQuestion -> {
            Question question = questionRepository.findById(testQuestion.getQuestionId());
            List<Choice> choices = choiceRepository.query().eq(Choice::getQuestionId, question.getId()).list();
            return StudentQuestionResponse.from(question, choices);
        }).toList();
    }
}
