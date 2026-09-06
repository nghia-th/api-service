package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for both {@code POST /api/parent/subjects} and {@code PUT /api/parent/subjects/{id}}
 * - the same 2 fields either way, so one DTO is reused for create and update (unlike Student,
 * which splits create/update because most Student fields are optional on update - here both
 * fields stay on the same DTO both ways, so a PUT always resends the full object, including
 * {@code classroomId}: this also means a Subject CAN be moved to a different Classroom the same
 * Parent owns, or made shared / unshared, via a normal update call).
 * <p>
 * <b>Revision 2026-09-06 (c):</b> {@code classroomId} is no longer required - per the user's
 * request for a Subject that applies to every Classroom of a Parent ("anh có 1 môn học mà không
 * thuộc lớp nào... các học sinh của phụ huynh đều học môn này không phân biệt lớp"), leaving it
 * {@code null} now means "shared across every Classroom of the current Parent" instead of
 * rejecting the request - see {@code Subject}'s and {@code SubjectService}'s javadoc.
 */
@Data
public class SubjectRequest {

    @Schema(type = "integer", example = "1", description = "Id of the Classroom this subject belongs to - must be owned by the current parent. Omit/null to make this Subject SHARED across every Classroom of the current parent.")
    private Long classroomId;

    @NotBlank
    @Schema(type = "string", example = "Math", description = "Subject name")
    private String name;
}
