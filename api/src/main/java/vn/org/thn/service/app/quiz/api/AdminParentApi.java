package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.AdminParentSummary;
import vn.org.thn.service.app.quiz.dto.AdminResetPasswordRequest;
import vn.org.thn.service.app.quiz.dto.AdminSetActiveRequest;
import vn.org.thn.service.app.quiz.dto.ParentRegisterRequest;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.AdminParentService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Admin's Parent-account management: list/create/activate-deactivate/reset-password/delete
 * (reset-password added 2026-09-04, per the user's explicit request). Every endpoint here is
 * behind {@link JwtAuthFilter} under {@code /api/admin/*}, so only a valid ADMIN-role token can
 * reach it (see {@code config/SecurityConfig}'s URL patterns) - the caller's own identity is never
 * used for ownership filtering the way {@code CurrentUser.get().userId()} is elsewhere in this
 * codebase (a Parent only ever sees THEIR OWN data); an Admin's token instead grants access to
 * EVERY Parent, by design.
 * <p>
 * See {@link AdminParentService}'s javadoc for the actual mechanics of deactivation (force-logout),
 * reset-password (Parent's own sessions only, no Student cascade), and deletion (full
 * unconditional cascade, per the user's explicit scoping decision).
 */
@Tag(name = "Admin - Parents", description = "Admin management of Parent accounts")
@RestController
@RequestMapping("/api/admin/parents")
public class AdminParentApi extends BaseCtl {

    @Autowired
    private AdminParentService adminParentService;

    @Operation(
            summary = "List every Parent account",
            description = "No pagination in v1 - the Admin UI sorts/filters client-side, same as every other 'list everything for this owner' endpoint elsewhere in the app."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every Parent, each with active/createdAt"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token, or not an ADMIN token - QUIZ_001 UNAUTHORIZED")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminParentSummary>>> list() {
        return ok(adminParentService.list());
    }

    @Operation(
            summary = "Create a Parent account",
            description = "Admin-created - unlike POST /api/auth/parent/register, does NOT auto-login the new account (an Admin is creating this for someone else)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created - returns the new Parent's info (password excluded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field (fullName/email/password) is missing or malformed - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email is already registered to another Parent - QUIZ_002 EMAIL_TAKEN")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AdminParentSummary>> create(@Valid @RequestBody ParentRegisterRequest request) {
        return ok(adminParentService.create(request));
    }

    @Operation(
            summary = "Activate or deactivate a Parent account",
            description = "Deactivating immediately force-logs-out the Parent AND every one of their Students (not just at their next login attempt) - see AdminParentService's javadoc."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active flag updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No Parent with this id - COMMON_005 NOT_FOUND")
    })
    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<AdminParentSummary>> setActive(
            @Parameter(description = "Parent id") @PathVariable Long id,
            @Valid @RequestBody AdminSetActiveRequest request) {
        return ok(adminParentService.setActive(id, request));
    }

    @Operation(
            summary = "Reset a Parent's password",
            description = "Admin types the new password directly (same UX as creating a Parent) - immediately force-logs-out the Parent's OWN sessions only (not their Students, since a password reset doesn't touch Student credentials) - see AdminParentService#resetPassword's javadoc."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset, Parent's sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "newPassword fails validation - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No Parent with this id - COMMON_005 NOT_FOUND")
    })
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Parameter(description = "Parent id") @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        adminParentService.resetPassword(id, request);
        return ok();
    }

    @Operation(
            summary = "Delete a Parent account (cascade, no blocking rule)",
            description = "Permanently removes the Parent and EVERY piece of data under them (Students, Classrooms, Subjects, Lessons, Questions, Choices, Tests, Attempts, refresh tokens) - unconditional, per the user's explicit scoping decision. Cannot be undone."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No Parent with this id - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Parent id") @PathVariable Long id) {
        adminParentService.deleteCascade(id);
        return ok();
    }
}
