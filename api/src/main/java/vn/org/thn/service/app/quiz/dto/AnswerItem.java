package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** One answer inside {@link AnswerRequest#getAnswers()}. A question the student has not answered yet is simply omitted from the list, not sent with a null choiceId - see task 6 spec. */
@Data
public class AnswerItem {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "10", description = "Question being answered - must be one of the attempt's test questions")
    private Long questionId;

    @NotNull
    @Schema(type = "integer", format = "int64", example = "42", description = "Chosen choice - must belong to questionId")
    private Long choiceId;
}
