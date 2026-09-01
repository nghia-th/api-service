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
 * {@code @Size(min = 2)} enforces "at least 2 choices"; the separate "exactly 1 correct choice"
 * rule cannot be expressed as a single-field Bean Validation constraint (it depends on every
 * element together), so it is checked in {@code QuestionService} instead - see {@code
 * QuizErrorCode#QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE}.
 * <p>
 * {@code hideContentInTest} was added 2026-09-01 for the "listening question" feature - it goes
 * through this same create/update request (set together with {@code content}/{@code
 * knowledgeTag}/choices) unlike the audio FILE itself, which is a separate upload endpoint (same
 * "text fields here, file via its own endpoint" split {@code Lesson} already uses for its
 * illustrative image).
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

    @NotNull
    @Size(min = 2, message = "A question needs at least 2 choices")
    @Valid
    @Schema(description = "The question's choices - at least 2, with exactly one marked correct")
    private List<ChoiceRequest> choices;

    @Schema(type = "boolean", example = "false", description = "When this question has an audio clip (task \"Cau hoi dang am thanh\", 2026-09-01): true hides 'content' from the Student's take-test screen so they must rely on the audio alone. No effect when the question has no audio yet. Optional - null/omitted means false (content always shown), same as every existing question before this field existed.")
    private Boolean hideContentInTest;
}
