package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One row of {@link AttemptReportResponse#getAnswers()} - Parent-facing, so unlike task 6's
 * student view, this DOES show the correct answer next to what the student actually chose.
 * <p>
 * {@code questionType}/{@code hasSpeakingAnswer}/{@code parentMarkedCorrect} were added
 * 2026-09-01 for the "speaking question" feature - for a SPEAKING row, {@code
 * chosenChoiceContent}/{@code correctChoiceContent} are always null (there are no choices) and
 * {@code correct} is always false (never meaningfully graded, see {@code QuestionType}'s javadoc
 * - the Parent-facing UI must branch on {@code questionType} rather than read {@code correct} for
 * this row). {@code hasSpeakingAnswer} tells the client whether to show a playback control
 * (audio fetched separately, {@code GET .../speaking-answer}, same "flag here / bytes via their
 * own endpoint" split as every other file attachment). {@code parentMarkedCorrect} is the tri-
 * state reference grade (null = not reviewed) - see {@code AttemptAnswer}'s javadoc.
 * <p>
 * {@code answerText}/{@code answerMode}/{@code referenceAnswer} were added 2026-09-01 for the
 * typed-essay alternative (see {@code AnswerMode}'s javadoc). {@code answerText} is the Student's
 * typed answer (sent inline, unlike the recorded audio which stays behind its own GET endpoint -
 * plain text is cheap to include directly). {@code referenceAnswer} is the Parent's OWN optional
 * model-answer note entered at question-authoring time ({@code Question#referenceAnswer}) - shown
 * here purely so the Parent can compare it against what the Student actually answered while
 * reviewing, never used for any auto-grading.
 */
@Data
@AllArgsConstructor
public class AttemptAnswerDetail {
    private Long questionId;
    private String questionContent;
    /** Null if the student left this question blank. */
    private String chosenChoiceContent;
    private String correctChoiceContent;
    private boolean correct;
    private String knowledgeTag;
    private String questionType;
    private boolean hasSpeakingAnswer;
    private Boolean parentMarkedCorrect;
    private String answerText;
    private String answerMode;
    private String referenceAnswer;
}
