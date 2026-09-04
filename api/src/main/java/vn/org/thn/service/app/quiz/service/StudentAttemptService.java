package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.AnswerItem;
import vn.org.thn.service.app.quiz.dto.AnswerRequest;
import vn.org.thn.service.app.quiz.dto.QuestionAudio;
import vn.org.thn.service.app.quiz.dto.QuestionVideo;
import vn.org.thn.service.app.quiz.dto.SpeakingAnswerAudio;
import vn.org.thn.service.app.quiz.dto.StartAttemptResponse;
import vn.org.thn.service.app.quiz.dto.KnowledgeTagBreakdown;
import vn.org.thn.service.app.quiz.dto.StudentAttemptAnswerDetail;
import vn.org.thn.service.app.quiz.dto.StudentAttemptReportResponse;
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
import vn.org.thn.service.app.quiz.entity.QuestionType;
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
import vn.org.thn.service.base.db.DatabasePath;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    /** 10 MB app-level cap for a Student's recorded SPEAKING answer - same cap as {@code QuestionService#MAX_AUDIO_SIZE_BYTES} for a Question's own listening-prompt clip. */
    private static final long MAX_SPEAKING_ANSWER_SIZE_BYTES = 10L * 1024 * 1024;

    // "audio/webm" duoc them rieng vi trinh duyet (MediaRecorder API) khi Hoc sinh ghi am cau tra loi
    // thuong tao ra dung dinh dang nay (Chrome/Firefox/Edge) - khong the doi sang mp3/wav/m4a ma
    // khong transcode phia server, nen chap nhan luon webm nhu 1 dinh dang hop le.
    private static final Map<String, String> ALLOWED_SPEAKING_ANSWER_TYPES = Map.of(
            "audio/mpeg", "mp3",
            "audio/mp4", "m4a",
            "audio/wav", "wav",
            "audio/x-wav", "wav",
            "audio/ogg", "ogg",
            "audio/webm", "webm"
    );

    /** Same folder-per-upload-kind layout as {@code QuestionService#AUDIO_DIR} - a Student's recorded answer is a different kind of file (own folder) even though it is the same audio formats. */
    private static final Path SPEAKING_ANSWER_DIR = DatabasePath.HOME.resolve("uploads").resolve("speaking-answers");

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

    /**
     * The current Student's own read-only review of one already-submitted attempt - {@code GET
     * /api/student/tests/{testId}/answers} (added 2026-09-02, per the user's explicit request:
     * "xem lai dap an nhung de da lam"). Mirrors {@code ReportService#getAttemptReport} (task 7,
     * Parent-facing) almost exactly - same per-question loop, same {@code
     * QUIZ_013 ATTEMPT_NOT_SUBMITTED} gate - but scoped to the CURRENT student's own Test (never
     * trusts an attemptId directly, resolves via testId + {@link #getOwnedTestOrThrow}, the same
     * ownership shape {@link #start} already uses) and mapped onto {@link
     * StudentAttemptAnswerDetail} instead of the Parent's {@code AttemptAnswerDetail} - that
     * mapping deliberately drops {@code Question#referenceAnswer} (the Parent's own private note,
     * never meant for the Student, see that DTO's javadoc). Only reachable once the attempt has
     * been submitted - revealing the correct answer mid-attempt would defeat the entire point of
     * {@link StudentChoiceResponse} never carrying {@code correct}.
     */
    public StudentAttemptReportResponse getOwnAttemptReport(Long testId) {
        Long studentId = CurrentUser.get().userId();
        Test test = getOwnedTestOrThrow(testId, studentId);

        Attempt attempt = attemptRepository.query().eq(Attempt::getTestId, testId).one();
        if (attempt == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "No attempt for this test yet");
        }
        // Defense in depth (Attempt.studentId should always match Test.studentId already checked
        // by getOwnedTestOrThrow above) - same "check both, never trust one implies the other"
        // caution ReportService#getStudentAttemptHistory already applies to Test/Parent.
        if (!attempt.getStudentId().equals(studentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This attempt does not belong to the current student");
        }
        if (attempt.getSubmittedAt() == null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_NOT_SUBMITTED);
        }

        List<TestQuestion> testQuestions = testQuestionRepository.query().eq(TestQuestion::getTestId, testId).list();
        testQuestions.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));

        Map<Long, AttemptAnswer> answerByQuestionId = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attempt.getId()).list().stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, answer -> answer));

        List<StudentAttemptAnswerDetail> details = new ArrayList<>();
        // LinkedHashMap so byKnowledgeTag comes out in the order tags first appear, same reasoning
        // as ReportService#getAttemptReport's tagStats.
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
            String tag = (question.getKnowledgeTag() == null || question.getKnowledgeTag().isBlank())
                    ? "Chưa phân loại"
                    : question.getKnowledgeTag();

            String questionType = question.getQuestionType() == null ? QuestionType.MULTIPLE_CHOICE.name() : question.getQuestionType();
            boolean hasSpeakingAnswer = answer != null && answer.getAnswerAudioPath() != null;
            Boolean parentMarkedCorrect = answer == null ? null : answer.getParentMarkedCorrect();
            String answerText = answer == null ? null : answer.getAnswerText();
            String answerMode = question.getAnswerMode();

            details.add(new StudentAttemptAnswerDetail(
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
                    answerMode));

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

        return new StudentAttemptReportResponse(
                attempt.getId(),
                test.getName(),
                test.getTestType(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                scorePercent,
                attempt.getSubmittedAt(),
                details,
                byKnowledgeTag);
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

    /**
     * Video-question feature (2026-09-04, part 3/4) - same access check as {@link
     * #getQuestionAudio} right above (a Question is reachable precisely because it is on some
     * Test assigned to the current Student), just loading the video file instead of the audio one.
     */
    public QuestionVideo getQuestionVideo(Long questionId) {
        Long studentId = CurrentUser.get().userId();
        Question question = questionService.getById(questionId);

        List<Long> testIds = testQuestionRepository.query().eq(TestQuestion::getQuestionId, questionId).list()
                .stream().map(TestQuestion::getTestId).distinct().toList();
        boolean accessible = !testIds.isEmpty()
                && testRepository.query().in(Test::getId, testIds).eq(Test::getStudentId, studentId).exists();
        if (!accessible) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Question video is not accessible");
        }

        return questionService.loadVideo(question);
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
            // totalQuestions counts only auto-graded (MULTIPLE_CHOICE) questions, NOT every
            // question on the test - a SPEAKING question is never part of the score denominator
            // (task "Cau hoi dang tu luan/thu am", 2026-09-01: "khong tinh diem, chi de tham
            // khao"), see #submit's matching recomputation and countScorableQuestions's javadoc.
            attempt.setTotalQuestions((int) countScorableQuestions(testQuestions));
            attempt.setCreatedAt(now);
            attempt.setUpdatedAt(now);
            attempt.setCreatedBy("student:" + studentId);
            attempt.setUpdatedBy("student:" + studentId);
            attempt = attemptRepository.save(attempt);
            logInfo("Attempt started: id={}, testId={}, studentId={}", attempt.getId(), testId, studentId);
        }

        return new StartAttemptResponse(attempt.getId(), studentQuestionsOf(testId, attempt.getId()));
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
        int scorableCount = 0;
        for (TestQuestion testQuestion : testQuestions) {
            Long questionId = testQuestion.getQuestionId();
            Question question = questionRepository.findById(questionId);
            AttemptAnswer answer = answerByQuestionId.get(questionId);
            if (answer == null) {
                // Never answered at all - still recorded as a blank/wrong (or, for SPEAKING,
                // simply blank) AttemptAnswer row so task 7's report has one row per question,
                // per the task 6 spec.
                answer = new AttemptAnswer();
                answer.setAttemptId(attemptId);
                answer.setQuestionId(questionId);
            }

            if (question != null && QuestionType.SPEAKING.name().equals(question.getQuestionType())) {
                // Never auto-graded and never counted toward correctCount/totalQuestions - see
                // QuestionType's javadoc ("khong tinh diem, chi de tham khao"). correct/
                // parentMarkedCorrect are left exactly as they already are (correct was always
                // null for a SPEAKING row; parentMarkedCorrect cannot be set yet at this point,
                // grading only opens up after submission - see ReportService#gradeSpeakingAnswer).
                attemptAnswerRepository.save(answer);
                continue;
            }

            scorableCount++;
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
        // Recomputed here too (not just trusted from #start) so totalQuestions/correctCount can
        // never disagree about which questions count, even though the test's own question set
        // cannot actually change between start and submit in v1.
        attempt.setTotalQuestions(scorableCount);
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

    /**
     * Uploads/replaces the Student's recorded voice answer for one SPEAKING question within an
     * in-progress attempt - {@code POST
     * /api/student/attempts/{attemptId}/questions/{questionId}/speaking-answer}. Same content-
     * type/size validation shape as {@code QuestionService#uploadAudio}. Can be called again any
     * number of times before submit to delete-and-redo the recording (the user's explicit answer
     * when this feature was scoped: re-answer allowed "chi trong luc dang lam bai, truoc khi
     * nop") - each call simply replaces whatever was recorded before.
     */
    @Transactional
    public void uploadSpeakingAnswer(Long attemptId, Long questionId, MultipartFile file) {
        Long studentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedAttemptOrThrow(attemptId, studentId);
        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }
        Question question = getSpeakingQuestionOfAttemptOrThrow(attempt, questionId);

        String extension = ALLOWED_SPEAKING_ANSWER_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(QuizErrorCode.SPEAKING_ANSWER_INVALID_TYPE);
        }
        if (file.getSize() > MAX_SPEAKING_ANSWER_SIZE_BYTES) {
            throw new BusinessException(QuizErrorCode.SPEAKING_ANSWER_TOO_LARGE);
        }

        try {
            Files.createDirectories(SPEAKING_ANSWER_DIR);
        } catch (IOException e) {
            logError("Could not create speaking answer directory " + SPEAKING_ANSWER_DIR, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        AttemptAnswer answer = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId)
                .eq(AttemptAnswer::getQuestionId, questionId)
                .one();
        if (answer == null) {
            answer = new AttemptAnswer();
            answer.setAttemptId(attemptId);
            answer.setQuestionId(questionId);
        }
        String oldAudioPath = answer.getAnswerAudioPath();

        String filename = "answer-" + attemptId + "-" + questionId + "-" + UUID.randomUUID() + "." + extension;
        Path target = SPEAKING_ANSWER_DIR.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            logError("Could not save speaking answer to " + target, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        answer.setAnswerAudioPath(filename);
        attemptAnswerRepository.save(answer);

        // Only removed AFTER the new file is safely written+saved, same "never leave a dangling
        // reference on a mid-upload failure" reasoning as QuestionService#uploadAudio.
        deleteSpeakingAnswerFileQuietly(oldAudioPath);

        logInfo("Speaking answer uploaded: attemptId={}, questionId={}, studentId={}, filename={}",
                attemptId, questionId, studentId, filename);
    }

    /**
     * Clears the Student's recorded answer for one SPEAKING question so they can record again
     * from a blank state - {@code DELETE
     * /api/student/attempts/{attemptId}/questions/{questionId}/speaking-answer}. No-op if there
     * was nothing recorded yet. Same "locked once submitted" rule as {@link
     * #uploadSpeakingAnswer}.
     */
    @Transactional
    public void deleteSpeakingAnswer(Long attemptId, Long questionId) {
        Long studentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedAttemptOrThrow(attemptId, studentId);
        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }
        getSpeakingQuestionOfAttemptOrThrow(attempt, questionId);

        AttemptAnswer answer = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId)
                .eq(AttemptAnswer::getQuestionId, questionId)
                .one();
        if (answer == null || answer.getAnswerAudioPath() == null) {
            return;
        }
        deleteSpeakingAnswerFileQuietly(answer.getAnswerAudioPath());
        answer.setAnswerAudioPath(null);
        attemptAnswerRepository.save(answer);
        logInfo("Speaking answer deleted: attemptId={}, questionId={}, studentId={}", attemptId, questionId, studentId);
    }

    /**
     * Saves (or clears, when {@code text} is null/blank) the Student's TYPED answer for a
     * SPEAKING question - {@code PUT
     * /api/student/attempts/{attemptId}/questions/{questionId}/speaking-answer/text} (2026-09-01,
     * typed-essay alternative to voice recording - see {@code AnswerMode}'s javadoc). Independent
     * of {@link #uploadSpeakingAnswer}/{@link #deleteSpeakingAnswer} (the recorded-audio answer) -
     * a question's {@code answerMode} is only a UI hint for which control(s) the take-test screen
     * shows (see {@code AnswerMode}'s javadoc); this endpoint itself does not enforce it, so a
     * typed answer can always be saved for any SPEAKING question regardless of its configured
     * mode. Same "locked once submitted" rule as the audio answer.
     */
    @Transactional
    public void saveSpeakingTextAnswer(Long attemptId, Long questionId, String text) {
        Long studentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedAttemptOrThrow(attemptId, studentId);
        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException(QuizErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }
        getSpeakingQuestionOfAttemptOrThrow(attempt, questionId);

        AttemptAnswer answer = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId)
                .eq(AttemptAnswer::getQuestionId, questionId)
                .one();
        if (answer == null) {
            answer = new AttemptAnswer();
            answer.setAttemptId(attemptId);
            answer.setQuestionId(questionId);
        }
        answer.setAnswerText(text == null || text.isBlank() ? null : text);
        attemptAnswerRepository.save(answer);
        logInfo("Speaking text answer saved: attemptId={}, questionId={}, studentId={}", attemptId, questionId, studentId);
    }

    /**
     * The Student's own playback of their recorded answer - {@code GET
     * /api/student/attempts/{attemptId}/questions/{questionId}/speaking-answer}. Unlike upload/
     * delete, this is NOT blocked by submission - a Student may want to listen back both while
     * still answering and after submitting (same "read access survives submission" shape as
     * {@code StudentLessonService}/the listening-question audio endpoint).
     */
    public SpeakingAnswerAudio getOwnSpeakingAnswerAudio(Long attemptId, Long questionId) {
        Long studentId = CurrentUser.get().userId();
        Attempt attempt = getOwnedAttemptOrThrow(attemptId, studentId);
        getSpeakingQuestionOfAttemptOrThrow(attempt, questionId);

        AttemptAnswer answer = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId)
                .eq(AttemptAnswer::getQuestionId, questionId)
                .one();
        if (answer == null || answer.getAnswerAudioPath() == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "No speaking answer recorded yet");
        }
        return loadSpeakingAnswerAudio(answer);
    }

    /**
     * Reads a recorded speaking answer's bytes off disk. Package-private + takes the already-
     * resolved {@link AttemptAnswer} (no ownership check of its own) so {@code ReportService} can
     * reuse it once it has independently proven, via its own Test-ownership check, that the
     * current Parent may hear this answer - same reuse shape as {@code
     * QuestionService#loadAudio}/{@code StudentAttemptService#getQuestionAudio}.
     */
    SpeakingAnswerAudio loadSpeakingAnswerAudio(AttemptAnswer answer) {
        if (answer.getAnswerAudioPath() == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "No speaking answer recorded yet");
        }
        Path path = SPEAKING_ANSWER_DIR.resolve(answer.getAnswerAudioPath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            logError("Speaking answer file missing on disk: " + path, e);
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Speaking answer file not found");
        }
        return new SpeakingAnswerAudio(bytes, contentTypeForSpeakingAnswerFilename(answer.getAnswerAudioPath()), answer.getAnswerAudioPath());
    }

    private String contentTypeForSpeakingAnswerFilename(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_SPEAKING_ANSWER_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(ext))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }

    /** Best-effort delete - a missing/already-gone file is not an error worth failing the caller's request over, same reasoning as every other upload's quiet-delete helper in this codebase. */
    private void deleteSpeakingAnswerFileQuietly(String audioPath) {
        if (audioPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(SPEAKING_ANSWER_DIR.resolve(audioPath));
        } catch (IOException e) {
            log().warn("Could not delete old speaking answer file {}: {}", audioPath, e.getMessage());
        }
    }

    /** Loads {@code questionId}, throwing if it is not part of {@code attempt}'s test (INVALID_PARAMETER) or is not a SPEAKING question (QUESTION_NOT_SPEAKING_TYPE) - shared guard for every speaking-answer endpoint above. */
    private Question getSpeakingQuestionOfAttemptOrThrow(Attempt attempt, Long questionId) {
        boolean onThisTest = testQuestionRepository.query()
                .eq(TestQuestion::getTestId, attempt.getTestId())
                .eq(TestQuestion::getQuestionId, questionId)
                .exists();
        if (!onThisTest) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                    "questionId " + questionId + " is not part of this attempt's test");
        }
        Question question = questionRepository.findById(questionId);
        if (question == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question not found");
        }
        if (!QuestionType.SPEAKING.name().equals(question.getQuestionType())) {
            throw new BusinessException(QuizErrorCode.QUESTION_NOT_SPEAKING_TYPE);
        }
        return question;
    }

    /** Count of {@code testQuestions} whose Question is NOT a SPEAKING type - the score denominator (see {@link #start}/{@link #submit}), since a SPEAKING question is never auto-graded or counted toward the score, per {@link QuestionType}'s javadoc. */
    private long countScorableQuestions(List<TestQuestion> testQuestions) {
        return testQuestions.stream()
                .map(tq -> questionRepository.findById(tq.getQuestionId()))
                .filter(question -> question != null && !QuestionType.SPEAKING.name().equals(question.getQuestionType()))
                .count();
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

    /**
     * The test's questions in display order, mapped to the no-correct-answer Student view - see
     * {@code StudentChoiceResponse}. {@code attemptId} was added 2026-09-01 to look up each
     * SPEAKING question's already-saved typed answer (if any) for resume support - see {@code
     * StudentQuestionResponse#answerText}'s javadoc for why this is fetched eagerly here rather
     * than lazily like the recorded-audio answer.
     */
    private List<StudentQuestionResponse> studentQuestionsOf(Long testId, Long attemptId) {
        List<TestQuestion> testQuestions = testQuestionRepository.query().eq(TestQuestion::getTestId, testId).list();
        testQuestions.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));

        Map<Long, AttemptAnswer> answerByQuestionId = attemptAnswerRepository.query()
                .eq(AttemptAnswer::getAttemptId, attemptId).list().stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, answer -> answer));

        return testQuestions.stream().map(testQuestion -> {
            Question question = questionRepository.findById(testQuestion.getQuestionId());
            List<Choice> choices = choiceRepository.query().eq(Choice::getQuestionId, question.getId()).list();
            AttemptAnswer answer = answerByQuestionId.get(question.getId());
            String answerText = answer == null ? null : answer.getAnswerText();
            return StudentQuestionResponse.from(question, choices, answerText);
        }).toList();
    }
}
