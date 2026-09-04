package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for {@code POST /api/auth/admin/login}. No Admin self-registration - see {@code entity/Admin.java}'s javadoc. */
@Data
public class AdminLoginRequest {

    @NotBlank
    @Schema(type = "string", example = "admin@example.com", description = "Admin email")
    private String email;

    @NotBlank
    @Schema(type = "string", example = "Secret123", description = "Password")
    private String password;
}
