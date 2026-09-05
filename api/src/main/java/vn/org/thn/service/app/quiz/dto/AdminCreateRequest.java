package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/admin/admins} (2026-09-05) - root-only, see {@code
 * AdminManageService#create}'s javadoc. Same shape as {@link ParentRegisterRequest} minus {@code
 * phone} (an Admin account has no use for it, unlike Parent) - a newly-created Admin here is
 * ALWAYS a regular (non-root) account, there is no {@code root} field to set here: {@code root} is
 * true for exactly one row, the bootstrap account created by {@code AdminBootstrapRunner}, never
 * for one created through this endpoint.
 */
@Data
public class AdminCreateRequest {

    @NotBlank
    @Schema(type = "string", example = "Jane Doe", description = "Admin's full name")
    private String fullName;

    @NotBlank
    @Email
    @Schema(type = "string", example = "admin2@example.com", description = "Login email, unique across the system")
    private String email;

    // Same minimum as ParentRegisterRequest#password - keep the two rules in sync if this ever changes.
    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(type = "string", example = "Secret123", description = "Password, minimum 6 characters (hashed with BCrypt before storage)")
    private String password;
}
