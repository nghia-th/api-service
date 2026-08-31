package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for both {@code POST /api/parent/subjects} and {@code PUT /api/parent/subjects/{id}} - the same single field either way, so one DTO is reused for create and update. */
@Data
public class SubjectRequest {

    @NotBlank
    @Schema(type = "string", example = "Math", description = "Subject name")
    private String name;
}
