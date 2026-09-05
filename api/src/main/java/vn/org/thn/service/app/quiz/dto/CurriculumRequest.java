package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for both {@code POST /api/admin/curricula} and {@code PUT /api/admin/curricula/{id}} - same single-field reuse pattern as {@code ClassroomRequest}. */
@Data
public class CurriculumRequest {

    @NotBlank
    @Schema(type = "string", example = "Ket noi tri thuc", description = "Curriculum name")
    private String name;
}
