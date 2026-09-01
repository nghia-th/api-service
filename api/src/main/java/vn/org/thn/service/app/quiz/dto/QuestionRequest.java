package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request body for both {@code POST /api/parent/questions} and {@code PUT
 * /api/parent/questions/{id}} - the task 4 spec describes the PUT body as "same as POST" (a full
 * replace of the question's content and choices), so one DTO is reused for create and update,
 * same reasoning as {@code SubjectRequest} in task 3.
 * <p>
 * {@code choices} lost its {@code @NotNull @Size(min = 2)} bean validation 2026-09-01 when the
 * "speaking question" feature (see {@code QuestionType}) needed it to be legitimately empty for
 * a SPEAKING question - that check, plus "exactly 1 correct choice" (which could never be a
 * single-field Bean Validation constraint anyway, it depends on every element together), both now
 * live in {@code QuestionService#validateChoices}, applied only when {@code questionType} is
 * MULTIPLE_CHOICE - see {@code QuizErrorCode#QUESTION_CHOICES_REQUIRED}/{@code
 * #QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE}.
 * <p>
 * {@code hideContentInTest} was added 2026-09-01 for the "listening question" feature - it goes
 * through this same create/update request (set together with {@code content}/{@code
 * knowledgeTag}/choices) unlike the audio FILE itself, which is a separate upload endpoint (same
 * "text fields here, file via its own endpoint" split {@code Lesson} already uses for its
 * illustrative image). {@code questionType} was added the same day for the "speaking question"
 * feature - see {@code QuestionType}'s javadoc.
 * <p>
 * {@code answerMode}/{@code referenceAnswer} were added 2026-09-01 - see {@code AnswerMode}'s
 * javadoc and {@code Question#referenceAnswer}'s javadoc respectively. Both are only meaningful
 * when {@code questionType} is SPEAKING; {@code QuestionService} nulls them out for MULTIPLE_CHOICE
 * regardless of what a caller sends.
 */
@Data
public class QuestionRequest {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "1", description = "Id of the lesson this question belongs to - must belong to the current parent")
    private Long lessonId;

    @NotBlank
    @Schema(type = "string", example = "What does the sun do every morning?", description = "Question text")
    private String content;

    @Schema(type = "string", example = "Do/Does", description = "Optional free-text knowledge tag, used later to group results by topic")
    private String knowledgeTag;

    @Valid
    @Schema(description = "The question's choices. Required (at least 2, with exactly one marked correct) when questionType is MULTIPLE_CHOICE (or omitted, the default); ignored/must be empty when questionType is SPEAKING - see QuestionService#validateChoices.", nullable = true)
    private List<ChoiceRequest> choices;

    @Schema(type = "boolean", example = "false", description = "When this question has an audio clip (task \"Cau hoi dang am thanh\", 2026-09-01): true hides 'content' from the Student's take-test screen so they must rely on the audio alone. No effect when the question has no audio yet. Optional - null/omitted means false (content always shown), same as every existing question before this field existed.")
    private Boolean hideContentInTest;

    @Schema(type = "string", example = "MULTIPLE_CHOICE", allowableValues = {"MULTIPLE_CHOICE", "SPEAKING"}, description = "Question type (task \"Cau hoi dang tu luan/thu am\", 2026-09-01). Optional - null/omitted means MULTIPLE_CHOICE, same as every existing question before this field existed. SPEAKING questions carry no choices and are answered by the Student recording their voice instead - see QuestionType's javadoc.", nullable = true)
    private String questionType;

    @Schema(type = "string", example = "AUDIO", allowableValues = {"AUDIO", "TEXT", "BOTH"}, description = "How the Student may answer a SPEAKING question (2026-09-01) - AUDIO = record voice only (original v1 behavior), TEXT = typed answer only, BOTH = either/both. Optional - null/omitted means AUDIO. Ignored (stored as null) when questionType is MULTIPLE_CHOICE.", nullable = true)
    private String answerMode;

    @Size(max = 10000)
    @Schema(type = "string", description = "Optional reference/model answer text (2026-09-01) - purely for the Parent's own later comparison when reviewing a SPEAKING answer, never shown to the Student, never auto-graded. Ignored (stored as null) when questionType is MULTIPLE_CHOICE.", nullable = true)
    private String referenceAnswer;
}
