package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.AttemptAnswerDetail;
import vn.org.thn.service.app.quiz.dto.AttemptReportResponse;
import vn.org.thn.service.app.quiz.dto.KnowledgeTagBreakdown;
import vn.org.thn.service.app.quiz.dto.StudentAttemptHistoryItem;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Test;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptAnswerRepository;
import vn.org.thn.service.app.quiz.repository.AttemptRepository;
import vn.org.thn.service.app.quiz.repository.ChoiceRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only results/reporting for the currently logged-in Parent (task 7) - the feature the whole
 * product concept centers on: not just a score, but which knowledge area the mistakes are in (see
 * {@code claude/hieu-bai-app-phan-tich.md}). No new entities - reads {@link Attempt}/{@link
 * AttemptAnswer}/{@link Question} written by task 6.
 * <p>
 * Ownership reuses {@link TestService#getOwnedOrThrow} (an Attempt's owner is its Test's
 * {@code parentId}) and {@link StudentService#getOwnedOrThrow} - both package-private, same reuse
 * pattern used throughout tasks 3-6.
 */
@Service
public class ReportService extends IBase {

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestService testService;

    @Autowired
    private StudentService studentService;

    public AttemptReportResponse getAttemptReport(Long attemptId) {
        Long parentId = CurrentUser.get().userId();
        Attempt attempt = attemptRepository.findById(attemptId);
        if (attempt == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Attempt not found");
        }
        Test test = testService.getOwnedOrThrow(attempt.getTestId(), parentId);
        if (attempt.getSubmittedAt() == null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_NOT_SUBMITTED);
        }

        Student student = studentRepository.findById(attempt.getStudentId());

        List<TestQuestion> testQuestions = testQuestionRepository.query().eq(TestQuestion::getTestId, attempt.getTestId()).list();
        testQuestions.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));

        Map<Long, AttemptAnswer> answerByQuestionId = new HashMap<>();
        for (AttemptAnswer answer : attemptAnswerRepository.query().eq(AttemptAnswer::getAttemptId, attemptId).list()) {
            answerByQuestionId.put(answer.getQuestionId(), answer);
        }

        List<AttemptAnswerDetail> details = new ArrayList<>();
        // LinkedHashMap so byKnowledgeTag comes out in the order tags first appear (stable,
        // readable) rather than an arbitrary hash order.
        Map<String, int[]> tagStats = new LinkedHashMap<>();

        for (TestQuestion testQuestion : testQuestions) {
            Question question = questionRepository.findById(testQuestion.getQuestionId());
            AttemptAnswer answer = answerByQuestionId.get(testQuestion.getQuestionId());
            Choice chosenChoice = (answer != null && answer.getChoiceId() != null)
                    ? choiceRepository.findById(answer.getChoiceId())
                    : null;
            Choice correctChoice = choiceRepository.query()
                    .eq(Choice::getQuestionId, question.getId())
                    .eq(Choice::getCorrect, true)
                    .one();
            boolean correct = answer != null && Boolean.TRUE.equals(answer.getCorrect());
            // Uncategorized questions are grouped together, not dropped from the report - see
            // KnowledgeTagBreakdown's javadoc for why this label is Vietnamese.
            String tag = (question.getKnowledgeTag() == null || question.getKnowledgeTag().isBlank())
                    ? "Chưa phân loại"
                    : question.getKnowledgeTag();

            details.add(new AttemptAnswerDetail(
                    question.getId(),
                    question.getContent(),
                    chosenChoice == null ? null : chosenChoice.getContent(),
                    correctChoice == null ? null : correctChoice.getContent(),
                    correct,
                    tag));

            int[] stat = tagStats.computeIfAbsent(tag, key -> new int[2]);
            stat[1]++;
            if (correct) {
                stat[0]++;
            }
        }

        List<KnowledgeTagBreakdown> byKnowledgeTag = tagStats.entrySet().stream()
                .map(entry -> new KnowledgeTagBreakdown(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();

        double scorePercent = (attempt.getTotalQuestions() == null || attempt.getTotalQuestions() == 0)
                ? 0.0
                : attempt.getCorrectCount() * 100.0 / attempt.getTotalQuestions();

        return new AttemptReportResponse(
                attempt.getId(),
                test.getName(),
                student == null ? null : student.getFullName(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                scorePercent,
                attempt.getSubmittedAt(),
                details,
                byKnowledgeTag);
    }

    /** A Student's test history - submitted attempts only, newest first (see {@link StudentAttemptHistoryItem}'s javadoc for the "submitted only" assumption). */
    public List<StudentAttemptHistoryItem> getStudentAttemptHistory(Long studentId) {
        Long parentId = CurrentUser.get().userId();
        studentService.getOwnedOrThrow(studentId, parentId);

        // Filtering Test by both studentId AND parentId (not just studentId) so a data
        // inconsistency elsewhere could never leak another parent's test into this history.
        Map<Long, Test> testById = new HashMap<>();
        for (Test test : testRepository.query().eq(Test::getStudentId, studentId).eq(Test::getParentId, parentId).list()) {
            testById.put(test.getId(), test);
        }

        return attemptRepository.query().eq(Attempt::getStudentId, studentId).list().stream()
                .filter(attempt -> attempt.getSubmittedAt() != null && testById.containsKey(attempt.getTestId()))
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .map(attempt -> new StudentAttemptHistoryItem(
                        attempt.getId(),
                        testById.get(attempt.getTestId()).getName(),
                        attempt.getSubmittedAt(),
                        attempt.getCorrectCount(),
                        attempt.getTotalQuestions(),
                        testById.get(attempt.getTestId()).getTestType()))
                .toList();
    }
}
