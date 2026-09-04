package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.AdminAuthResponse;
import vn.org.thn.service.app.quiz.dto.AdminLoginRequest;
import vn.org.thn.service.app.quiz.dto.ParentAuthResponse;
import vn.org.thn.service.app.quiz.dto.ParentLoginRequest;
import vn.org.thn.service.app.quiz.dto.ParentRegisterRequest;
import vn.org.thn.service.app.quiz.dto.RefreshTokenRequest;
import vn.org.thn.service.app.quiz.dto.StudentAuthResponse;
import vn.org.thn.service.app.quiz.dto.StudentLoginRequest;
import vn.org.thn.service.app.quiz.dto.TokenPairResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.AuthService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

/**
 * The only 6 endpoints NOT behind {@link JwtAuthFilter} - everything else under {@code
 * /api/parent/**}/{@code /api/student/**}/{@code /api/admin/**} requires a valid bearer token
 * (see {@code config/SecurityConfig}'s URL patterns, which simply never match {@code
 * /api/auth/**}). {@code refresh}/{@code logout} (2026-09-04) join the original 3 register/login
 * endpoints here for the same reason: a caller with no valid access token yet (expired, or never
 * logged in) must still be able to reach them - see {@code AuthService}'s javadoc for the
 * refresh-token/force-logout design this is part of. {@code admin/login} (2026-09-04) is here for
 * the same reason too, but has NO matching {@code admin/register} - see {@code entity/Admin.java}
 * for why there's no Admin self-registration. "Force logout ALL sessions" ({@code logoutAll}) is
 * different - it needs to know WHICH account to log out, so it lives behind the filter instead,
 * see {@code SessionApi}.
 * <p>
 * {@code @Valid} on every request body triggers Bean Validation (the {@code @NotBlank}/{@code
 * @Email}/{@code @Size} annotations on the DTOs) - a failure is caught by {@code base}'s {@code
 * GlobalExceptionHandler#handleValidationException} before this method body ever runs, so
 * {@code AuthService} no longer needs to blank-check these fields itself.
 * <p>
 * {@code @Operation}/{@code @ApiResponses}/{@code @Tag} document this controller for Swagger UI
 * (already wired up in {@code base} - see {@code springdoc.swagger-ui.path} in
 * {@code application.yaml}, and {@code io.swagger.v3.oas.annotations.responses.ApiResponse}'s
 * name collides with our own {@link ApiResponse} response envelope, hence the fully-qualified
 * {@code @io.swagger.v3.oas.annotations.responses.ApiResponse} instead of a normal import).
 */
@Tag(name = "Auth", description = "Parent/Student/Admin register and login - the endpoints that don't require a token")
@RestController
@RequestMapping("/api/auth")
public class AuthApi extends BaseCtl {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Parent self-registration",
            description = "Creates a new Parent and returns a token right away (auto-login), so no separate login call is needed after registering."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registered successfully - returns a token plus the Parent's info (password excluded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field (fullName/email/password) is missing or malformed - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email is already registered to another Parent - QUIZ_002 EMAIL_TAKEN")
    })
    @PostMapping("/parent/register")
    public ResponseEntity<ApiResponse<ParentAuthResponse>> registerParent(@Valid @RequestBody ParentRegisterRequest request) {
        return ok(authService.registerParent(request));
    }

    @Operation(
            summary = "Parent login",
            description = "Returns the token used by every endpoint under /api/parent/**."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged in successfully - returns a token plus the Parent's info"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "email/password missing - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email not found or wrong password - one shared error, never reveals which one it was - QUIZ_004 INVALID_CREDENTIALS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This account has been deactivated by an administrator - QUIZ_027 ACCOUNT_DEACTIVATED")
    })
    @PostMapping("/parent/login")
    public ResponseEntity<ApiResponse<ParentAuthResponse>> loginParent(@Valid @RequestBody ParentLoginRequest request) {
        return ok(authService.loginParent(request));
    }

    @Operation(
            summary = "Student login",
            description = "There is no student self-registration - an account is only created via POST /api/parent/students (task 2). Returns the token used by every endpoint under /api/student/**."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged in successfully - returns a token plus the Student's info"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "username/password missing - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Username not found or wrong password - one shared error - QUIZ_004 INVALID_CREDENTIALS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The owning Parent's account has been deactivated by an administrator - QUIZ_027 ACCOUNT_DEACTIVATED")
    })
    @PostMapping("/student/login")
    public ResponseEntity<ApiResponse<StudentAuthResponse>> loginStudent(@Valid @RequestBody StudentLoginRequest request) {
        return ok(authService.loginStudent(request));
    }

    @Operation(
            summary = "Admin login",
            description = "No self-registration - the first (and, in v1, only) Admin row is bootstrapped at startup, see AdminBootstrapRunner. Returns the token used by every endpoint under /api/admin/**."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged in successfully - returns a token plus the Admin's info"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "email/password missing - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email not found or wrong password - one shared error - QUIZ_004 INVALID_CREDENTIALS")
    })
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AdminAuthResponse>> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return ok(authService.loginAdmin(request));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a still-valid refresh token for a new access token; the refresh token itself is rotated in the same call (the one just spent is revoked, a brand-new one returned) - see AuthService's javadoc."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New access token + rotated refresh token issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "refreshToken missing - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token unknown/expired/already used, or the owning account no longer exists/is deactivated - QUIZ_026 REFRESH_TOKEN_INVALID")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPairResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ok(authService.refresh(request.getRefreshToken()));
    }

    @Operation(
            summary = "Logout (this device/session only)",
            description = "Revokes the given refresh token. The current access token (if any) keeps working until its own short expiry - use POST /api/parent/logout-all, /api/student/logout-all or /api/admin/logout-all (SessionApi) to invalidate every session immediately instead."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Always succeeds, even if the token was already unknown/revoked/expired - never reveals which")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ok();
    }
}
