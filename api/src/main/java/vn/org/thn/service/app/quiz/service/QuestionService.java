package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.ChoiceRequest;
import vn.org.thn.service.app.quiz.dto.QuestionAudio;
import vn.org.thn.service.app.quiz.dto.QuestionRequest;
import vn.org.thn.service.app.quiz.dto.QuestionVideo;
import vn.org.thn.service.app.quiz.dto.QuestionResponse;
import vn.org.thn.service.app.quiz.entity.AnswerMode;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.QuestionType;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptAnswerRepository;
import vn.org.thn.service.app.quiz.repository.ChoiceRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Question/Choice CRUD for the currently logged-in Parent (task 4), by hand via this class - the
 * file-import path (also task 4) is a separate entry point, {@link QuestionImportService}, that
 * calls back into this class's {@link #createFromImportRow} to reuse the exact same choice-
 * validation and persistence logic rather than duplicating it.
 * <p>
 * {@code lessonId} ownership is resolved through {@link LessonService#getOwnedOrThrow} (package-
 * private, same reuse pattern {@code LessonService} itself uses on {@code SubjectService}) - a
 * Question has no {@code parentId} of its own, same reasoning as {@code Lesson} not having one.
 * <p>
 * IMPORTANT: {@code update} mutates the entity loaded by {@link #getOwnedOrThrow} in place rather
 * than building a fresh {@code Question} instance - {@code base}'s {@code InsertExecutor#save}
 * UPDATE branch writes every non-primary-key column straight from the object it is given, so a
 * freshly-built object (with {@code createdAt}/{@code createdBy} left null) would silently wipe
 * those audit columns to NULL on every update. Same pattern already used correctly by {@code
 * StudentService}/{@code SubjectService}/{@code LessonService} in tasks 2-3.
 * <p>
 * BUG FIX 2026-09-01 (real error hit while testing, not hypothetical): {@code update} always
 * deletes every existing {@link Choice} for the Question and recreates the new set from scratch
 * (see that method's own comment - true and harmless at task 4 time, when this class's javadoc
 * said Choice "has no external references of its own"). That stopped being true once task 6 added
 * {@link AttemptAnswer#getChoiceId()}, a foreign key to {@code choice.id} - editing a Question
 * that a Student has already answered hit a raw {@code fk_attempt_answer_choice} PostgreSQL
 * constraint violation (500) instead of a friendly business error. Fixed by {@link
 * #ensureNotYetAttempted}, called at the top of {@link #update} before anything is mutated -
 * blocks the WHOLE update once any of the Question's current Choices has been picked in an
 * Attempt, same "block outright rather than partially allow" philosophy as {@code
 * TEST_HAS_ATTEMPTS} blocking Test deletion. This is not just a workaround for the FK: silently
 * rewriting a Question/its Choices out from under a Student's already-graded history would make
 * that Student's stored {@code AttemptAnswer.correct}/Parent-facing report retroactively
 * inaccurate, so blocking the edit is the semantically correct behavior, not merely the
 * convenient one.
 */
@Service
public class QuestionService extends IBase {

    /** 10 MB app-level cap for a question's audio clip - same "deliberately smaller than Spring's own max-file-size" reasoning as {@code LessonService#MAX_IMAGE_SIZE_BYTES}, sized up from that 5MB image cap since a short listening-question audio clip runs larger than a photo. */
    private static final long MAX_AUDIO_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_AUDIO_TYPES = Map.of(
            "audio/mpeg", "mp3",
            "audio/mp4", "m4a",
            "audio/wav", "wav",
            "audio/x-wav", "wav",
            "audio/ogg", "ogg"
    );

    /** Same folder-per-upload-kind layout as {@code LessonService#IMAGE_DIR} - see that field's javadoc for why this lives outside {@link DatabasePath}. */
    private static final Path AUDIO_DIR = DatabasePath.HOME.resolve("uploads").resolve("questions");

    /** 50 MB app-level cap for a question's video clip (2026-09-04, part 3/4) - well under the 80MB Spring {@code max-file-size} in application.yaml (same "business limit smaller than the servlet-level reject threshold" reasoning as {@link #MAX_AUDIO_SIZE_BYTES}), sized up from that 10MB audio cap since a short illustrative video clip runs larger than an audio-only recording. */
    private static final long MAX_VIDEO_SIZE_BYTES = 50L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_VIDEO_TYPES = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov",
            "video/ogg", "ogv"
    );

    /** Separate folder from {@link #AUDIO_DIR} (own upload kind, own file naming) - same layout convention. */
    private static final Path VIDEO_DIR = DatabasePath.HOME.resolve("uploads").resolve("question-videos");

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        Long parentId = CurrentUser.get().userId();
        lessonService.getOwnedOrThrow(request.getLessonId(), parentId);
        String questionType = normalizeQuestionType(request.getQuestionType());
        List<ChoiceRequest> validChoices = validateChoices(questionType, request.getChoices());
        String answerMode = normalizeAnswerMode(questionType, request.getAnswerMode());
        String referenceAnswer = normalizeReferenceAnswer(questionType, request.getReferenceAnswer());

        Question question = questionRepository.save(newQuestion(parentId, request.getLessonId(),
                request.getContent(), request.getKnowledgeTag(), request.getHideContentInTest(), questionType,
                answerMode, referenceAnswer));
        List<Choice> choices = saveChoices(question.getId(), validChoices);

        logInfo("Question created: id={}, lessonId={}, parentId={}, questionType={}", question.getId(), question.getLessonId(), parentId, questionType);
        return QuestionResponse.from(question, choices);
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        ensureNotYetAttempted(id);
        lessonService.getOwnedOrThrow(request.getLessonId(), parentId);
        String questionType = normalizeQuestionType(request.getQuestionType());
        List<ChoiceRequest> validChoices = validateChoices(questionType, request.getChoices());
        String answerMode = normalizeAnswerMode(questionType, request.getAnswerMode());
        String referenceAnswer = normalizeReferenceAnswer(questionType, request.getReferenceAnswer());

        question.setLessonId(request.getLessonId());
        question.setContent(request.getContent());
        question.setKnowledgeTag(request.getKnowledgeTag());
        question.setHideContentInTest(Boolean.TRUE.equals(request.getHideContentInTest()));
        question.setQuestionType(questionType);
        question.setAnswerMode(answerMode);
        question.setReferenceAnswer(referenceAnswer);
        question.setUpdatedAt(LocalDateTime.now());
        question.setUpdatedBy("parent:" + parentId);
        question = questionRepository.save(question);

        // Simplest correct approach for v1 (per task 4 spec): replace every choice rather than
        // diffing old vs. new - a Question's choices are small in number and have no external
        // references of their own (no separate "Choice API"). For a SPEAKING question validChoices
        // is always empty (see validateChoices), so this simply clears out any leftover choices
        // from before it was switched from MULTIPLE_CHOICE, if ever.
        choiceRepository.delete().eq(Choice::getQuestionId, id).execute();
        List<Choice> choices = saveChoices(question.getId(), validChoices);

        logInfo("Question updated: id={}, parentId={}, questionType={}", question.getId(), parentId, questionType);
        return QuestionResponse.from(question, choices);
    }

    public QuestionResponse get(Long id) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        return QuestionResponse.from(question, choicesOf(id));
    }

    /** Every Question under {@code lessonId}, each with its full Choice list (including which one is correct - Parent-facing only, see {@code QuestionResponse}). */
    public List<QuestionResponse> list(Long lessonId) {
        Long parentId = CurrentUser.get().userId();
        lessonService.getOwnedOrThrow(lessonId, parentId);
        return questionRepository.query().eq(Question::getLessonId, lessonId).list().stream()
                .map(question -> QuestionResponse.from(question, choicesOf(question.getId())))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);

        if (testQuestionRepository.query().eq(TestQuestion::getQuestionId, id).exists()) {
            throw new BusinessException(QuizErrorCode.QUESTION_USED_IN_TEST);
        }
        choiceRepository.delete().eq(Choice::getQuestionId, id).execute();
        deleteAudioFileQuietly(question.getAudioPath());
        deleteVideoFileQuietly(question.getVideoPath());
        questionRepository.deleteById(question.getId());
        logInfo("Question deleted: id={}, parentId={}", question.getId(), parentId);
    }

    /**
     * Validates and stores a new audio clip for the question, replacing any previous one - same
     * shape as {@code LessonService#uploadImage} (content-type checked against {@link
     * #ALLOWED_AUDIO_TYPES}, never the client-supplied filename/extension; size checked against
     * {@link #MAX_AUDIO_SIZE_BYTES}; server-generated filename, sidestepping path traversal and any
     * client-controlled extension). Blocked by {@link #ensureNotYetAttempted} first, same as {@link
     * #update} - see the class javadoc's "BUG FIX 2026-09-01" note: replacing a Question's audio
     * after a Student has already answered it would make that Student's "xem lai bai hoc"/report
     * review play back different audio than what they actually heard, the exact same integrity
     * problem {@code QUESTION_HAS_ATTEMPTS} already exists to prevent for {@code content}/choices.
     */
    public QuestionResponse uploadAudio(Long id, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        ensureNotYetAttempted(id);

        String extension = ALLOWED_AUDIO_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(QuizErrorCode.QUESTION_AUDIO_INVALID_TYPE);
        }
        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new BusinessException(QuizErrorCode.QUESTION_AUDIO_TOO_LARGE);
        }

        try {
            Files.createDirectories(AUDIO_DIR);
        } catch (IOException e) {
            logError("Could not create question audio directory " + AUDIO_DIR, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        String oldAudioPath = question.getAudioPath();
        String filename = "question-" + id + "-" + UUID.randomUUID() + "." + extension;
        Path target = AUDIO_DIR.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            logError("Could not save question audio to " + target, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        question.setAudioPath(filename);
        question.setUpdatedAt(LocalDateTime.now());
        question.setUpdatedBy("parent:" + parentId);
        question = questionRepository.save(question);

        // Only removed AFTER the new file is safely written+saved, same "never leave a dangling
        // reference on a mid-upload failure" reasoning as LessonService#uploadImage.
        deleteAudioFileQuietly(oldAudioPath);

        logInfo("Question audio uploaded: id={}, parentId={}, filename={}", id, parentId, filename);
        return QuestionResponse.from(question, choicesOf(id));
    }

    /** Only the owning Parent can view it. Throws {@code COMMON_005 NOT_FOUND} if the question has no audio yet. */
    public QuestionAudio getAudioOwned(Long id, Long parentId) {
        Question question = getOwnedOrThrow(id, parentId);
        return loadAudio(question);
    }

    /** Same {@link #ensureNotYetAttempted} guard as {@link #uploadAudio} - see that method's javadoc. */
    public QuestionResponse deleteAudio(Long id) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        ensureNotYetAttempted(id);

        deleteAudioFileQuietly(question.getAudioPath());
        question.setAudioPath(null);
        question.setUpdatedAt(LocalDateTime.now());
        question.setUpdatedBy("parent:" + parentId);
        question = questionRepository.save(question);

        logInfo("Question audio deleted: id={}, parentId={}", id, parentId);
        return QuestionResponse.from(question, choicesOf(id));
    }

    /**
     * Reads the question's audio bytes off disk. Package-private + takes the already-resolved
     * {@link Question} (no ownership check of its own) so {@code StudentAttemptService} can reuse
     * it once it has independently proven, via its own {@code TestQuestion}-based check, that the
     * current Student may hear this Question's audio - same reuse shape as {@code
     * LessonService#loadImage}/{@code StudentLessonService}.
     */
    QuestionAudio loadAudio(Question question) {
        if (question.getAudioPath() == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question has no audio");
        }
        Path path = AUDIO_DIR.resolve(question.getAudioPath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            logError("Question audio file missing on disk: " + path, e);
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question audio file not found");
        }
        return new QuestionAudio(bytes, contentTypeForAudioFilename(question.getAudioPath()), question.getAudioPath());
    }

    private String contentTypeForAudioFilename(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_AUDIO_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(ext))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }

    /** Best-effort delete - a missing/already-gone file is not an error worth failing the caller's request over. */
    private void deleteAudioFileQuietly(String audioPath) {
        if (audioPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(AUDIO_DIR.resolve(audioPath));
        } catch (IOException e) {
            log().warn("Could not delete old question audio file {}: {}", audioPath, e.getMessage());
        }
    }

    /**
     * Validates and stores a new video clip for the question, replacing any previous one - same
     * shape/reasoning as {@link #uploadAudio} (2026-09-04, part 3/4 - "video question" feature,
     * file-upload only per AskUserQuestion). Blocked by {@link #ensureNotYetAttempted} first, same
     * integrity reasoning: replacing a Question's video after a Student has already answered it
     * would make that Student's "xem lai bai hoc"/report review show a different video than what
     * they actually watched.
     */
    public QuestionResponse uploadVideo(Long id, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        ensureNotYetAttempted(id);

        String extension = ALLOWED_VIDEO_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(QuizErrorCode.QUESTION_VIDEO_INVALID_TYPE);
        }
        if (file.getSize() > MAX_VIDEO_SIZE_BYTES) {
            throw new BusinessException(QuizErrorCode.QUESTION_VIDEO_TOO_LARGE);
        }

        try {
            Files.createDirectories(VIDEO_DIR);
        } catch (IOException e) {
            logError("Could not create question video directory " + VIDEO_DIR, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        String oldVideoPath = question.getVideoPath();
        String filename = "question-" + id + "-" + UUID.randomUUID() + "." + extension;
        Path target = VIDEO_DIR.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            logError("Could not save question video to " + target, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        question.setVideoPath(filename);
        question.setUpdatedAt(LocalDateTime.now());
        question.setUpdatedBy("parent:" + parentId);
        question = questionRepository.save(question);

        // Only removed AFTER the new file is safely written+saved, same reasoning as uploadAudio.
        deleteVideoFileQuietly(oldVideoPath);

        logInfo("Question video uploaded: id={}, parentId={}, filename={}", id, parentId, filename);
        return QuestionResponse.from(question, choicesOf(id));
    }

    /** Only the owning Parent can view it. Throws {@code COMMON_005 NOT_FOUND} if the question has no video yet. */
    public QuestionVideo getVideoOwned(Long id, Long parentId) {
        Question question = getOwnedOrThrow(id, parentId);
        return loadVideo(question);
    }

    /** Same {@link #ensureNotYetAttempted} guard as {@link #uploadVideo} - see that method's javadoc. */
    public QuestionResponse deleteVideo(Long id) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        ensureNotYetAttempted(id);

        deleteVideoFileQuietly(question.getVideoPath());
        question.setVideoPath(null);
        question.setUpdatedAt(LocalDateTime.now());
        question.setUpdatedBy("parent:" + parentId);
        question = questionRepository.save(question);

        logInfo("Question video deleted: id={}, parentId={}", id, parentId);
        return QuestionResponse.from(question, choicesOf(id));
    }

    /**
     * Reads the question's video bytes off disk. Package-private + takes the already-resolved
     * {@link Question} (no ownership check of its own) so {@code StudentAttemptService} can reuse
     * it once it has independently proven the current Student may watch this Question's video -
     * same reuse shape as {@link #loadAudio}.
     */
    QuestionVideo loadVideo(Question question) {
        if (question.getVideoPath() == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question has no video");
        }
        Path path = VIDEO_DIR.resolve(question.getVideoPath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            logError("Question video file missing on disk: " + path, e);
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question video file not found");
        }
        return new QuestionVideo(bytes, contentTypeForVideoFilename(question.getVideoPath()), question.getVideoPath());
    }

    private String contentTypeForVideoFilename(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_VIDEO_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(ext))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }

    /** Best-effort delete - a missing/already-gone file is not an error worth failing the caller's request over. */
    private void deleteVideoFileQuietly(String videoPath) {
        if (videoPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(VIDEO_DIR.resolve(videoPath));
        } catch (IOException e) {
            log().warn("Could not delete old question video file {}: {}", videoPath, e.getMessage());
        }
    }

    /**
     * Creates one Question + its Choices for {@link QuestionImportService} - same persistence
     * logic as {@link #create}, minus re-deriving {@code parentId}/re-checking {@code lessonId}
     * ownership a second time per row (the import already resolved and verified both once for the
     * whole file) and minus the "exactly one correct choice" check (the import validates that
     * per-row itself, before calling this, so it can attach the row number to the error message -
     * see {@link QuestionImportService#importRow}).
     * <p>
     * {@code @Transactional} on this method only (not on the caller's import loop) - the task 4
     * spec wants best-effort per row, so one row's Question+Choices insert must be atomic with
     * itself but independent of every other row: a later row failing must not roll back an
     * earlier row's already-committed success.
     */
    @Transactional
    Question createFromImportRow(Long lessonId, Long parentId, String content, String knowledgeTag, List<ChoiceRequest> choices) {
        // Import always produces MULTIPLE_CHOICE questions - v1 does not support importing
        // SPEAKING questions from a file (the user's explicit answer when this feature was
        // scoped: "chi nhap tay + tai audio sau", the same "hand-entry only" decision already made
        // for the listening-question audio clip). answerMode/referenceAnswer are both null here -
        // meaningless for MULTIPLE_CHOICE (normalizeAnswerMode/normalizeReferenceAnswer would null
        // them out anyway, skipped here since the type is already known statically).
        Question question = questionRepository.save(newQuestion(parentId, lessonId, content, knowledgeTag, null, QuestionType.MULTIPLE_CHOICE.name(), "MULTIPLE_CHOICE", null));
        saveChoices(question.getId(), choices);
        return question;
    }

    /** Loads the Question with id {@code id}, throwing if it doesn't exist or its Lesson doesn't belong to {@code parentId}. Package-private (not private) so {@code TestService} (task 5) can validate a whole {@code questionIds} list against one parent, same pattern as {@code LessonService#getOwnedOrThrow}. */
    Question getOwnedOrThrow(Long id, Long parentId) {
        Question question = getById(id);
        lessonService.getOwnedOrThrow(question.getLessonId(), parentId);
        return question;
    }

    /** Loads the Question with id {@code id} with NO ownership check at all. Package-private so {@code StudentAttemptService} can resolve it after doing its own (Parent-unrelated) Student ownership check for {@link #loadAudio} - same reuse shape as {@code LessonService#getById}. */
    Question getById(Long id) {
        Question question = questionRepository.findById(id);
        if (question == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question not found");
        }
        return question;
    }

    private List<Choice> choicesOf(Long questionId) {
        return choiceRepository.query().eq(Choice::getQuestionId, questionId).list();
    }

    /**
     * Throws {@link QuizErrorCode#QUESTION_HAS_ATTEMPTS} if any of this Question's CURRENT
     * Choices has already been picked in an Attempt ({@code AttemptAnswer.choiceId} references
     * {@code Choice.id} via {@code fk_attempt_answer_choice}). Called at the top of {@link
     * #update} - see the class javadoc's "BUG FIX 2026-09-01" note for why this exists: {@link
     * #update} always deletes every current Choice before recreating the new set, which would
     * otherwise violate that foreign key with a raw DB error the moment any Choice has been
     * answered.
     */
    private void ensureNotYetAttempted(Long questionId) {
        List<Long> choiceIds = choiceRepository.query().eq(Choice::getQuestionId, questionId).list()
                .stream().map(Choice::getId).toList();
        if (!choiceIds.isEmpty() && attemptAnswerRepository.query().in(AttemptAnswer::getChoiceId, choiceIds).exists()) {
            throw new BusinessException(QuizErrorCode.QUESTION_HAS_ATTEMPTS);
        }
    }

    /** A brand-new (unsaved) Question with both audit timestamps set - only ever for a genuine INSERT (see the class javadoc for why update() must NOT go through this). audioPath is never set here - a brand-new Question never has an audio file yet, it can only be attached afterwards via {@link #uploadAudio} once the Question has an id, same 2-step "create, then attach the file" flow as Lesson's image. */
    private Question newQuestion(Long parentId, Long lessonId, String content, String knowledgeTag, Boolean hideContentInTest, String questionType, String answerMode, String referenceAnswer) {
        LocalDateTime now = LocalDateTime.now();
        Question question = new Question();
        question.setLessonId(lessonId);
        question.setContent(content);
        question.setKnowledgeTag(knowledgeTag);
        question.setHideContentInTest(Boolean.TRUE.equals(hideContentInTest));
        question.setQuestionType(questionType);
        question.setAnswerMode(answerMode);
        question.setReferenceAnswer(referenceAnswer);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        question.setCreatedBy("parent:" + parentId);
        question.setUpdatedBy("parent:" + parentId);
        return question;
    }

    private List<Choice> saveChoices(Long questionId, List<ChoiceRequest> choiceRequests) {
        List<Choice> saved = new ArrayList<>();
        for (ChoiceRequest choiceRequest : choiceRequests) {
            Choice choice = new Choice();
            choice.setQuestionId(questionId);
            choice.setContent(choiceRequest.getContent());
            choice.setCorrect(choiceRequest.getCorrect());
            saved.add(choiceRepository.save(choice));
        }
        return saved;
    }

    /**
     * Normalizes {@code questionType} from a request - null/blank means {@link
     * QuestionType#MULTIPLE_CHOICE} (every existing Parent app/client that predates this field),
     * otherwise it must be one of {@link QuestionType}'s exact names.
     */
    private String normalizeQuestionType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return QuestionType.MULTIPLE_CHOICE.name();
        }
        try {
            return QuestionType.valueOf(questionType.trim()).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                    "questionType must be MULTIPLE_CHOICE or SPEAKING, got: " + questionType);
        }
    }

    /**
     * Validates {@code choices} against {@code questionType} (added 2026-09-01 for the "speaking
     * question" feature) and returns the list to actually persist:
     * <ul>
     * <li>SPEAKING - always returns an empty list, regardless of what was sent. A SPEAKING
     * question never has choices (see {@link QuestionType}'s javadoc) - silently dropping any
     * choices a caller mistakenly sends is simpler than erroring on it, and harmless either way.
     * <li>MULTIPLE_CHOICE - requires at least 2 choices ({@link
     * QuizErrorCode#QUESTION_CHOICES_REQUIRED}, replaces the {@code @Size(min = 2)} bean
     * validation removed from {@code QuestionRequest#choices} since it could not be conditional
     * on questionType) with exactly one marked correct ({@link
     * QuizErrorCode#QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE}, unchanged from before this feature).
     * </ul>
     */
    private List<ChoiceRequest> validateChoices(String questionType, List<ChoiceRequest> choices) {
        if (QuestionType.SPEAKING.name().equals(questionType)) {
            return List.of();
        }
        if (choices == null || choices.size() < 2) {
            throw new BusinessException(QuizErrorCode.QUESTION_CHOICES_REQUIRED);
        }
        long correctCount = choices.stream().filter(choice -> Boolean.TRUE.equals(choice.getCorrect())).count();
        if (correctCount != 1) {
            throw new BusinessException(QuizErrorCode.QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE);
        }
        return choices;
    }

    /**
     * Normalizes {@code answerMode} (2026-09-01, typed-essay alternative - see {@link
     * AnswerMode}'s javadoc) - null for MULTIPLE_CHOICE (meaningless there, regardless of what a
     * caller sends), AUDIO for SPEAKING when null/blank (the original v1 behavior stays the
     * default so every pre-existing SPEAKING question keeps working exactly as before), otherwise
     * must be one of {@link AnswerMode}'s exact names.
     */
    private String normalizeAnswerMode(String questionType, String answerMode) {
        if (!QuestionType.SPEAKING.name().equals(questionType)) {
            return null;
        }
        if (answerMode == null || answerMode.isBlank()) {
            return AnswerMode.AUDIO.name();
        }
        try {
            return AnswerMode.valueOf(answerMode.trim()).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER,
                    "answerMode must be AUDIO, TEXT or BOTH, got: " + answerMode);
        }
    }

    /** Nulls out {@code referenceAnswer} for MULTIPLE_CHOICE (meaningless there); trims and passes it through as-is for SPEAKING, blank collapses to null. See {@code Question#referenceAnswer}'s javadoc. */
    private String normalizeReferenceAnswer(String questionType, String referenceAnswer) {
        if (!QuestionType.SPEAKING.name().equals(questionType) || referenceAnswer == null || referenceAnswer.isBlank()) {
            return null;
        }
        return referenceAnswer.trim();
    }
}
