package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.ChangePasswordRequest;
import vn.org.thn.service.app.quiz.service.AuthService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

/**
 * Self-service change-password (2026-09-04, per the user's explicit request), all 3 roles - see
 * {@code AuthService#changePassword}'s javadoc for the actual mechanics (old-password check, then
 * force-logout of every session including this one). Same "three thin endpoints, one shared
 * method reading {@code CurrentUser.get()}" shape as {@code SessionApi} - see that controller's
 * javadoc for why one endpoint per role instead of a single shared path ({@code JwtAuthFilter}
 * gates by URL prefix, so each role needs its own path under its own token type).
 * <p>
 * Not folded into {@code AuthApi} for the same reason as {@code SessionApi}: this needs a valid
 * token to know WHICH account to change, so it must sit behind {@code JwtAuthFilter}.
 */
@Tag(name = "Account", description = "Self-service change-password")
@RestController
public class AccountApi extends BaseCtl {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Change this Parent's own password",
            description = "Verifies oldPassword first, then saves newPassword and force-logs-out every session (this device included) - the frontend must treat a successful response like a manual logout."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed, all sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "oldPassword does not match, or newPassword fails validation - QUIZ_030 OLD_PASSWORD_INCORRECT / COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token - QUIZ_001 UNAUTHORIZED")
    })
    @PostMapping("/api/parent/change-password")
    public ResponseEntity<ApiResponse<Void>> changePasswordParent(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ok();
    }

    @Operation(
            summary = "Change this Student's own password",
            description = "Same as POST /api/parent/change-password, for a Student-role token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed, all sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "oldPassword does not match, or newPassword fails validation - QUIZ_030 OLD_PASSWORD_INCORRECT / COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token - QUIZ_001 UNAUTHORIZED")
    })
    @PostMapping("/api/student/change-password")
    public ResponseEntity<ApiResponse<Void>> changePasswordStudent(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ok();
    }

    @Operation(
            summary = "Change this Admin's own password",
            description = "Same as POST /api/parent/change-password, for an Admin-role token - this is how the bootstrapped root/root account's password gets changed (see AdminBootstrapRunner's javadoc)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed, all sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "oldPassword does not match, or newPassword fails validation - QUIZ_030 OLD_PASSWORD_INCORRECT / COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token - QUIZ_001 UNAUTHORIZED")
    })
    @PostMapping("/api/admin/change-password")
    public ResponseEntity<ApiResponse<Void>> changePasswordAdmin(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ok();
    }
}
