package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for {@code PUT /api/parent/lessons/{id}}. Unlike {@link SubjectRequest}'s reuse, this is a separate DTO from {@link LessonCreateRequest} because update cannot move a Lesson to a different Subject - only {@code name} is editable. */
@Data
public class LessonUpdateRequest {

    @NotBlank
    @Schema(type = "string", example = "Unit 1 - Present Simple (revised)", description = "New lesson name")
    private String name;
}
