package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code PUT /api/parent/students/{id}}. Every field is optional - null means
 * "leave unchanged", so a parent can update just one field (e.g. only reset the password) without
 * resending the rest. See {@code StudentService#update} for the null-vs-blank handling: a blank
 * (non-null) string is also treated as "leave unchanged" for fullName/username, while a blank
 * password is rejected by {@code @Size} below before it ever reaches the service. {@code
 * classroomId} follows the plain null-means-unchanged rule (no blank-string case - it's a Long).
 */
@Data
public class StudentUpdateRequest {

    @Schema(type = "string", example = "Jane Doe", description = "New full name, or omit/null to leave unchanged")
    private String fullName;

    @Schema(type = "integer", example = "1", description = "New Classroom id (must be owned by the current parent), or omit/null to leave unchanged")
    private Long classroomId;

    @Schema(type = "string", example = "student01", description = "New username (still checked for system-wide uniqueness), or omit/null to leave unchanged")
    private String username;

    @Size(min = 6, max = 100)
    @Schema(type = "string", example = "NewSecret456", description = "New password, minimum 6 characters, or omit/null to leave unchanged")
    private String password;
}
