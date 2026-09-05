package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/parent/set-username} and {@code POST /api/admin/set-username}
 * (2026-09-05) - lets a Parent/Admin who registered/was created without a username set one later,
 * per the user's explicit decision that existing accounts without a username should be able to
 * "add it later" rather than be backfilled automatically. See {@code AuthService#setUsername}.
 * <p>
 * Not offered to Student - a Student already always has a {@code username} (its only login
 * identifier since it has no email), set at creation time.
 */
@Data
public class SetUsernameRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(type = "string", example = "jane.parent", description = "New username, unique among accounts of the caller's own role (Parent or Admin)")
    private String username;
}
