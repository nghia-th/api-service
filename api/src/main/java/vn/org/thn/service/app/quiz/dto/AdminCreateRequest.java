package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/admin/admins} (2026-09-05) - root-only, see {@code
 * AdminManageService#create}'s javadoc. A newly-created Admin here is ALWAYS a regular (non-root)
 * account, there is no {@code root} field to set here: {@code root} is true for exactly one row,
 * the bootstrap account created by {@code AdminBootstrapRunner}, never for one created through
 * this endpoint.
 * <p>
 * {@code username}/{@code phone} (2026-09-05) - both OPTIONAL, per the user's explicit request
 * that Admin support logging in by email, username, OR phone (see {@code entity/Admin.java}'s
 * javadoc). Leave either blank to set it later via the self-service "set username" endpoint (
 * {@code phone} has no such endpoint yet - only settable here at creation time).
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

    @Size(max = 100)
    @Schema(type = "string", example = "jane.admin", description = "Optional alternate login identifier, unique among Admin accounts - leave blank to set later")
    private String username;

    @Schema(type = "string", example = "0912345678", description = "Optional phone number, usable as an alternate login identifier")
    private String phone;
}
