package vn.org.thn.service.app.quiz.entity;

/**
 * The 3 values {@link Question#getAnswerMode()} can hold - only meaningful when {@code
 * questionType} is {@link QuestionType#SPEAKING} (null/ignored for MULTIPLE_CHOICE, same "enum,
 * but the entity field itself stays a plain String" pattern as {@link QuestionType}/{@link
 * TestType}). Added 2026-09-01 after the Parent tested the original "chi ghi am giong noi" v1 and
 * asked for a typed-essay alternative too: "phu huynh co the cho hien thi loai tra loi, 1 thu am,
 * 2 tu luan, 3 ca 2 (hoc sinh co the tra loi bang thu am hay nhap tay tu luan)".
 * <p>
 * AUDIO - the original v1 behavior, and the default for every SPEAKING question created before
 * this field existed (backfilled by {@code V11__speaking_answer_mode.sql}) - the Student answers
 * only by recording their voice ({@code AttemptAnswer.answerAudioPath}).
 * <p>
 * TEXT - the Student answers only by typing ({@code AttemptAnswer.answerText}) - no microphone
 * involved at all for this question.
 * <p>
 * BOTH - the Student's take-test screen shows BOTH a record control and a text box; either one
 * (or both) may be filled in, there is no server-side rule forcing exactly one - "answered" for
 * this question simply means at least one of {@code answerAudioPath}/{@code answerText} is
 * non-blank (see {@code BlocStudentAttempt.ts}'s answered-count logic on the frontend).
 * <p>
 * IMPORTANT: this field is a UI HINT ONLY - {@code StudentAttemptService}'s save-audio/save-text
 * endpoints do not themselves enforce it (a Student can technically save a typed answer for an
 * AUDIO-only question, for example). Enforcing "only the configured mode's control is usable" is
 * the take-test screen's job, not the server's - same "server permissive, UI enforces the intended
 * flow" reasoning used elsewhere in this codebase, and it keeps a later mode change by the Parent
 * from ever locking out an answer the Student already gave under the old mode.
 */
public enum AnswerMode {
    AUDIO,
    TEXT,
    BOTH
}
