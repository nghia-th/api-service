package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for {@code POST /api/parent/students}. {@code parentId} is taken from {@link vn.org.thn.service.app.quiz.security.CurrentUser}, never from this body. */
@Data
public class StudentCreateRequest {

    @NotBlank
    @Schema(type = "string", example = "Jane Doe", description = "Student's full name")
    private String fullName;

    @NotBlank
    @Schema(type = "string", example = "Grade 3", description = "Free-text grade level, e.g. \"Grade 3\" (not a fixed enum in v1)")
    private String grade;

    @NotBlank
    @Schema(type = "string", example = "student01", description = "Login username - must be unique system-wide, not just within this parent's own students")
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(type = "string", example = "Secret123", description = "Password, minimum 6 characters (hashed with BCrypt before storage)")
    private String password;
}
