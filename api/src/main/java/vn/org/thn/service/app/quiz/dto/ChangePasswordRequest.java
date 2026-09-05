package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/{parent,student,admin}/change-password} (2026-09-04,
 * self-service change-password for all 3 roles - see {@code AuthService#changePassword}'s
 * javadoc for the actual mechanics). {@code oldPassword} is required so an already-logged-in
 * caller still has to prove they know the current password before setting a new one (same idea
 * as re-authenticating for a sensitive action) - a stolen/left-open session alone is not enough.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank
    @Schema(type = "string", example = "OldSecret123", description = "Current password - must match, or QUIZ_030 OLD_PASSWORD_INCORRECT is returned")
    private String oldPassword;

    // Same minimum as ParentRegisterRequest#password - keep the two rules in sync if this ever changes.
    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(type = "string", example = "NewSecret456", description = "New password, minimum 6 characters (hashed with BCrypt before storage)")
    private String newPassword;
}
