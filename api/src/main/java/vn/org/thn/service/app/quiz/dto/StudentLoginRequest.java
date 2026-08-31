package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for {@code POST /api/auth/student/login}. There is no student register request - see {@code AuthService}. */
@Data
public class StudentLoginRequest {

    @NotBlank
    @Schema(type = "string", example = "student01", description = "Login username, set by the parent when the student profile was created")
    private String username;

    @NotBlank
    @Schema(type = "string", example = "Secret123", description = "Password, set/reset by the parent")
    private String password;
}
