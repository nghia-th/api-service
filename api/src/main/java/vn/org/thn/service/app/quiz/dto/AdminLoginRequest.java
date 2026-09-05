package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /api/auth/admin/login}. No Admin self-registration - see {@code
 * entity/Admin.java}'s javadoc.
 * <p>
 * {@code identifier} (2026-09-05, renamed from {@code email}) - accepts the Admin's email,
 * username, OR phone, per the user's explicit request to allow logging in with any of the 3. See
 * {@code AuthService#loginAdmin} for the actual lookup (tries all 3 columns).
 */
@Data
public class AdminLoginRequest {

    @NotBlank
    @Schema(type = "string", example = "admin@example.com", description = "Admin's email, username, or phone")
    private String identifier;

    @NotBlank
    @Schema(type = "string", example = "Secret123", description = "Password")
    private String password;
}
