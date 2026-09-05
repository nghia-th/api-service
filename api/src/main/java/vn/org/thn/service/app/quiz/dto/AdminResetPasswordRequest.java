package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/admin/parents/{id}/reset-password} (2026-09-04) - the Admin
 * types the new password directly (same UX as {@code ParentRegisterRequest#password} on the
 * create-parent flow), unlike self-service {@code ChangePasswordRequest} there is no {@code
 * oldPassword} field: an Admin resetting another account's password does not know (and should
 * not need) the old one - the Admin's own token is already the proof of authority here.
 */
@Data
public class AdminResetPasswordRequest {

    // Same minimum as ParentRegisterRequest#password - keep the two rules in sync if this ever changes.
    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(type = "string", example = "NewSecret456", description = "New password for this Parent, minimum 6 characters (hashed with BCrypt before storage)")
    private String newPassword;
}
