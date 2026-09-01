package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** One choice inside {@link QuestionRequest#getChoices()}. */
@Data
public class ChoiceRequest {

    @NotBlank
    @Schema(type = "string", example = "4", description = "Choice text")
    private String content;

    @NotNull
    @Schema(type = "boolean", example = "true", description = "Whether this is the correct choice - exactly one choice in the list must be true")
    private Boolean correct;
}
