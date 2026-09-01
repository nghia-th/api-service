package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for both {@code POST /api/parent/classrooms} and {@code PUT /api/parent/classrooms/{id}} - the same single field either way, same reuse pattern as {@code SubjectRequest}. */
@Data
public class ClassroomRequest {

    @NotBlank
    @Schema(type = "string", example = "Lop 5A", description = "Classroom name")
    private String name;
}
