package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.ChoiceRequest;
import vn.org.thn.service.app.quiz.dto.QuestionRequest;
import vn.org.thn.service.app.quiz.dto.QuestionResponse;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;
import vn.org.thn.service.app.quiz.entity.TestQuestion;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AttemptAnswerRepository;
import vn.org.thn.service.app.quiz.repository.ChoiceRepository;
import vn.org.thn.service.app.quiz.repository.QuestionRepository;
import vn.org.thn.service.app.quiz.repository.TestQuestionRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        validateExactlyOneCorrectChoice(request.getChoices());

        Question question = questionRepository.save(
                newQuestion(parentId, request.getLessonId(), request.getContent(), request.getKnowledgeTag()));
        List<Choice> choices = saveChoices(question.getId(), request.getChoices());

        logInfo("Question created: id={}, lessonId={}, parentId={}", question.getId(), question.getLessonId(), parentId);
        return QuestionResponse.from(question, choices);
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Long parentId = CurrentUser.get().userId();
        Question question = getOwnedOrThrow(id, parentId);
        ensureNotYetAttempted(id);
        lessonService.getOwnedOrThrow(request.getLessonId(), parentId);
        validateExactlyOneCorrectChoice(request.getChoices());

        question.setLessonId(request.getLessonId());
        question.setContent(request.getContent());
        question.setKnowledgeTag(request.getKnowledgeTag());
        question.setUpdatedAt(LocalDateTime.now());
        question.setUpdatedBy("parent:" + parentId);
        question = questionRepository.save(question);

        // Simplest correct approach for v1 (per task 4 spec): replace every choice rather than
        // diffing old vs. new - a Question's choices are small in number and have no external
        // references of their own (no separate "Choice API").
        choiceRepository.delete().eq(Choice::getQuestionId, id).execute();
        List<Choice> choices = saveChoices(question.getId(), request.getChoices());

        logInfo("Question updated: id={}, parentId={}", question.getId(), parentId);
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
        questionRepository.deleteById(question.getId());
        logInfo("Question deleted: id={}, parentId={}", question.getId(), parentId);
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
        Question question = questionRepository.save(newQuestion(parentId, lessonId, content, knowledgeTag));
        saveChoices(question.getId(), choices);
        return question;
    }

    /** Loads the Question with id {@code id}, throwing if it doesn't exist or its Lesson doesn't belong to {@code parentId}. Package-private (not private) so {@code TestService} (task 5) can validate a whole {@code questionIds} list against one parent, same pattern as {@code LessonService#getOwnedOrThrow}. */
    Question getOwnedOrThrow(Long id, Long parentId) {
        Question question = questionRepository.findById(id);
        if (question == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Question not found");
        }
        lessonService.getOwnedOrThrow(question.getLessonId(), parentId);
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

    /** A brand-new (unsaved) Question with both audit timestamps set - only ever for a genuine INSERT (see the class javadoc for why update() must NOT go through this). */
    private Question newQuestion(Long parentId, Long lessonId, String content, String knowledgeTag) {
        LocalDateTime now = LocalDateTime.now();
        Question question = new Question();
        question.setLessonId(lessonId);
        question.setContent(content);
        question.setKnowledgeTag(knowledgeTag);
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

    /** Package-private so {@link QuestionImportService} can run the same check per row before calling {@link #createFromImportRow}. */
    void validateExactlyOneCorrectChoice(List<ChoiceRequest> choices) {
        long correctCount = choices.stream().filter(choice -> Boolean.TRUE.equals(choice.getCorrect())).count();
        if (correctCount != 1) {
            throw new BusinessException(QuizErrorCode.QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE);
        }
    }
}
