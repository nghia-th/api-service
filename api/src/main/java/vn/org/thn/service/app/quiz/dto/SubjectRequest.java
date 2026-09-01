package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for both {@code POST /api/parent/subjects} and {@code PUT /api/parent/subjects/{id}}
 * - the same 2 fields either way, so one DTO is reused for create and update (unlike Student,
 * which splits create/update because most Student fields are optional on update - here both
 * fields stay required both ways, so a PUT always resends the full object, including
 * {@code classroomId}: this also means a Subject CAN be moved to a different Classroom the same
 * Parent owns via a normal update call).
 */
@Data
public class SubjectRequest {

    @NotNull
    @Schema(type = "integer", example = "1", description = "Id of the Classroom this subject belongs to - must be owned by the current parent")
    private Long classroomId;

    @NotBlank
    @Schema(type = "string", example = "Math", description = "Subject name")
    private String name;
}
