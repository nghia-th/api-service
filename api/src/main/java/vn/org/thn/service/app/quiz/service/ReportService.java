package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.AttemptAnswerDetail;
import vn.org.thn.service.app.quiz.dto.AttemptReportResponse;
import vn.org.thn.service.app.quiz.dto.KnowledgeTagBreakdown;
import vn.org.thn.service.app.quiz.dto.SpeakingAnswerAudio;
import vn.org.thn.service.app.quiz.dto.StudentAttemptHistoryItem;
import vn.org.thn.service.app.quiz.entity.Attempt;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.QuestionType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private StudentAttemptService studentAttemptService;

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

            String questionType = question.getQuestionType() == null ? QuestionType.MULTIPLE_CHOICE.name() : question.getQuestionType();
            boolean hasSpeakingAnswer = answer != null && answer.getAnswerAudioPath() != null;
            Boolean parentMarkedCorrect = answer == null ? null : answer.getParentMarkedCorrect();
            // answerText/answerMode/referenceAnswer added 2026-09-01 for the typed-essay
            // alternative - see AnswerMode's javadoc and AttemptAnswerDetail's javadoc.
            String answerText = answer == null ? null : answer.getAnswerText();
            String answerMode = question.getAnswerMode();

            details.add(new AttemptAnswerDetail(
                    question.getId(),
                    question.getContent(),
                    chosenChoice == null ? null : chosenChoice.getContent(),
                    correctChoice == null ? null : correctChoice.getContent(),
                    correct,
                    tag,
                    questionType,
                    hasSpeakingAnswer,
                    parentMarkedCorrect,
                    answerText,
                    answerMode,
                    question.getReferenceAnswer()));

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
        return getStudentAttemptHistory(studentId, null, null);
    }

    /**
     * Same as {@link #getStudentAttemptHistory(Long)} but narrowed to a date range - added
     * 2026-09-05 (item 8 of the 11-item batch, "phu huynh xem duoc lich su hoc tap cua con trong 1
     * tuan") so the Parent-facing Reports page can show just the current week instead of the
     * entire history. {@code from}/{@code to} are both INCLUSIVE calendar dates in the server's
     * local time zone (matching {@link Attempt#getSubmittedAt()}, a plain {@code LocalDateTime}
     * with no zone info stored anywhere else in this codebase) - either or both may be {@code
     * null} to leave that end of the range open, so passing neither reproduces the original
     * unfiltered behavior exactly (kept as the public 1-arg overload above for every existing
     * caller).
     */
    public List<StudentAttemptHistoryItem> getStudentAttemptHistory(Long studentId, LocalDate from, LocalDate to) {
        Long parentId = CurrentUser.get().userId();
        studentService.getOwnedOrThrow(studentId, parentId);

        // Filtering Test by both studentId AND parentId (not just studentId) so a data
        // inconsistency elsewhere could never leak another parent's test into this history.
        Map<Long, Test> testById = new HashMap<>();
        for (Test test : testRepository.query().eq(Test::getStudentId, studentId).eq(Test::getParentId, parentId).list()) {
            testById.put(test.getId(), test);
        }

        // to.plusDays(1).atStartOfDay() (exclusive upper bound) rather than to.atTime(23,59,59) -
        // simpler and correct down to the nanosecond, same "exclusive next-day boundary" shape as
        // StudentTimetableService's date handling.
        LocalDateTime fromInclusive = from == null ? null : from.atStartOfDay();
        LocalDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay();

        return attemptRepository.query().eq(Attempt::getStudentId, studentId).list().stream()
                .filter(attempt -> attempt.getSubmittedAt() != null && testById.containsKey(attempt.getTestId()))
                .filter(attempt -> fromInclusive == null || !attempt.getSubmittedAt().isBefore(fromInclusive))
                .filter(attempt -> toExclusive == null || attempt.getSubmittedAt().isBefore(toExclusive))
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

    /**
     * The owning Parent's playback of a Student's recorded SPEAKING answer - {@code GET
     * /api/parent/attempts/{attemptId}/questions/{questionId}/speaking-answer} (task "Cau hoi
     * dang tu luan/thu am", 2026-09-01). Only reachable once the attempt has been submitted (same
     * {@code QUIZ_013 ATTEMPT_NOT_SUBMITTED} gate {@link #getAttemptReport} already applies) -
     * reviewing a mid-attempt recording is not a supported flow in v1. Reuses {@link
     * StudentAttemptService#loadSpeakingAnswerAudio} (package-private) rather than duplicating
     * file-reading logic here, same reuse shape {@code StudentAttemptService#getQuestionAudio}
     * already established for {@code QuestionService#loadAudio}.
     */
    public SpeakingAnswerAudio getSpeakingAnswerAudio(Long attemptId, Long questionId) {
        Long parentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedSubmittedAttemptOrThrow(attemptId, parentId);
        AttemptAnswer answer = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId)
                .eq(AttemptAnswer::getQuestionId, questionId)
                .one();
        if (answer == null || answer.getAnswerAudioPath() == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "No speaking answer recorded for this question");
        }
        return studentAttemptService.loadSpeakingAnswerAudio(answer);
    }

    /**
     * The owning Parent's own reference grade for a SPEAKING answer - {@code PUT
     * /api/parent/attempts/{attemptId}/questions/{questionId}/grade}. {@code correct == null}
     * clears it back to "not reviewed". Purely a note for the Parent's own report reading - NEVER
     * recomputes {@code Attempt.correctCount}/{@code scorePercent}, per the user's explicit
     * "khong tinh diem, chi de tham khao" answer when this feature was scoped (see {@code
     * AttemptAnswer#parentMarkedCorrect}'s javadoc). Only reachable once the attempt has been
     * submitted, same gate as {@link #getSpeakingAnswerAudio} - grading an answer the Student
     * might still change makes no sense.
     */
    public void gradeSpeakingAnswer(Long attemptId, Long questionId, Boolean correct) {
        Long parentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedSubmittedAttemptOrThrow(attemptId, parentId);

        Question question = questionRepository.findById(questionId);
        if (question == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question not found");
        }
        if (!QuestionType.SPEAKING.name().equals(question.getQuestionType())) {
            throw new BusinessException(QuizErrorCode.QUESTION_NOT_SPEAKING_TYPE);
        }

        AttemptAnswer answer = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId)
                .eq(AttemptAnswer::getQuestionId, questionId)
                .one();
        if (answer == null) {
            // The student may have left this question entirely blank - #submit still creates a
            // placeholder row for every question (see its javadoc), but guard here too in case
            // that ever changes, so grading never NPEs on a genuinely missing row.
            answer = new AttemptAnswer();
            answer.setAttemptId(attemptId);
            answer.setQuestionId(questionId);
        }
        answer.setParentMarkedCorrect(correct);
        attemptAnswerRepository.save(answer);
        logInfo("Speaking answer graded: attemptId={}, questionId={}, parentId={}, correct={}", attemptId, questionId, parentId, correct);
    }

    /** Loads the Attempt with id {@code attemptId}, throwing if it doesn't exist, its Test doesn't belong to {@code parentId}, or it has not been submitted yet - shared by {@link #getSpeakingAnswerAudio}/{@link #gradeSpeakingAnswer}. */
    private Attempt getOwnedSubmittedAttemptOrThrow(Long attemptId, Long parentId) {
        Attempt attempt = attemptRepository.findById(attemptId);
        if (attempt == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Attempt not found");
        }
        testService.getOwnedOrThrow(attempt.getTestId(), parentId);
        if (attempt.getSubmittedAt() == null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_NOT_SUBMITTED);
        }
        return attempt;
    }
}
