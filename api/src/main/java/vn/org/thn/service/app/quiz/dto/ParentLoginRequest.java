package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for {@code POST /api/auth/parent/login}. */
@Data
public class ParentLoginRequest {

    @NotBlank
    @Schema(type = "string", example = "parent@example.com", description = "Registered email")
    private String email;

    @NotBlank
    @Schema(type = "string", example = "Secret123", description = "Password")
    private String password;
}
