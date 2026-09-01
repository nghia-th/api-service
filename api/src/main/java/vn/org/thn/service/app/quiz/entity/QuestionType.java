package vn.org.thn.service.app.quiz.entity;

/**
 * The 2 values {@link Question#getQuestionType()} can hold - stored as its {@code name()} in the
 * plain {@code question_type} column, same "enum, but the entity field itself stays a plain
 * String" pattern as {@link TestType}/{@code Test#getTestType()} (see that class's javadoc for
 * the reasoning).
 * <p>
 * MULTIPLE_CHOICE - every Question that existed before this type was added (task 4, backfilled to
 * MULTIPLE_CHOICE by {@code V10__speaking_question.sql}) and every new one created the same way:
 * {@code choices} required, exactly one marked correct, auto-graded at {@code
 * StudentAttemptService#submit}.
 * <p>
 * SPEAKING - added 2026-09-01 ("cau hoi dang tu luan hoac thu am cau tra loi tu hoc sinh") so a
 * Parent can ask an open-ended question the Student answers by RECORDING THEIR VOICE (not
 * choosing from options) - e.g. "Hay tu gioi thieu ban than bang tieng Anh". Chosen over a
 * separate "essay" text-answer type per the user's explicit answer when this feature was scoped
 * ("chi ghi am giong noi") - the whole point is to get the child speaking out loud, not typing,
 * as practice for confidence rather than more multiple-choice. A SPEAKING question:
 * <ul>
 * <li>has NO {@link Choice}s at all - {@code choices} is always an empty list, never validated
 * for "at least 2"/"exactly one correct" (see {@code QuestionService#create}/{@code #update});
 * <li>can still optionally carry the EXISTING {@code Question.audioPath} listening-prompt clip
 * (task "Cau hoi dang am thanh") - the two features are orthogonal: a Parent can ask a spoken
 * English question via {@code audioPath} and have the Student answer by recording their own
 * voice back, a real "listen then speak" exercise;
 * <li>is never auto-graded - {@code AttemptAnswer.correct} stays null forever for this type (see
 * {@code StudentAttemptService#submit}), and is excluded from {@code Attempt.correctCount}/{@code
 * totalQuestions} entirely, per the user's explicit answer ("khong tinh diem, chi de tham khao");
 * <li>is excluded from "On tap kien thuc" random generation ({@code
 * TestService#questionIdsOfSubject}), per the user's explicit answer - practice stays multiple-
 * choice only;
 * <li>the Student's recorded answer lives in {@code AttemptAnswer.answerAudioPath} (uploaded via
 * its own multipart endpoint, same "file via its own endpoint, text fields via the JSON request"
 * split as every other file attachment in this codebase) and can be deleted/re-recorded freely
 * until the Attempt is submitted, never after (same "locked once submitted" rule as every other
 * answer);
 * <li>the owning Parent can optionally mark it correct/incorrect ({@code
 * AttemptAnswer.parentMarkedCorrect}, tri-state: null = not reviewed yet) after the Attempt is
 * submitted - purely a reference note for the Parent's own report reading, it NEVER changes {@code
 * Attempt.correctCount}/{@code scorePercent}, per the user's explicit answer.
 * </ul>
 */
public enum QuestionType {
    MULTIPLE_CHOICE,
    SPEAKING
}
