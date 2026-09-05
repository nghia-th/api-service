package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for {@code POST /api/auth/parent/register}. */
@Data
public class ParentRegisterRequest {

    @NotBlank
    @Schema(type = "string", example = "John Doe", description = "Parent's full name")
    private String fullName;

    @NotBlank
    @Email
    @Schema(type = "string", example = "parent@example.com", description = "Login email, unique across the system")
    private String email;

    // Minimum 6 chars - an assumption (not yet confirmed elsewhere in the docs), revisit if a different rule is needed.
    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(type = "string", example = "Secret123", description = "Password, minimum 6 characters (hashed with BCrypt before storage)")
    private String password;

    @Schema(type = "string", example = "0912345678", description = "Phone number (optional)")
    private String phone;

    @Size(max = 100)
    @Schema(type = "string", example = "jane.parent", description = "Optional alternate login identifier, unique among Parent accounts - leave blank to set later (2026-09-05)")
    private String username;
}
