package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request body for {@code POST /api/parent/lessons}. {@code subjectId} must belong to the current Parent (checked in {@code LessonService}), not just exist. */
@Data
public class LessonCreateRequest {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "1", description = "Id of the subject this lesson belongs to - must belong to the current parent")
    private Long subjectId;

    @NotBlank
    @Schema(type = "string", example = "Unit 1 - Present Simple", description = "Lesson name")
    private String name;
}
