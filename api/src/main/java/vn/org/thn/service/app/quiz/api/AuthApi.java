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
import vn.org.thn.service.app.quiz.dto.ParentAuthResponse;
import vn.org.thn.service.app.quiz.dto.ParentLoginRequest;
import vn.org.thn.service.app.quiz.dto.ParentRegisterRequest;
import vn.org.thn.service.app.quiz.dto.StudentAuthResponse;
import vn.org.thn.service.app.quiz.dto.StudentLoginRequest;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.AuthService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

/**
 * The only 3 endpoints NOT behind {@link JwtAuthFilter} - everything else under {@code
 * /api/parent/**}/{@code /api/student/**} requires a valid bearer token (see {@code
 * config/SecurityConfig}'s URL patterns, which simply never match {@code /api/auth/**}).
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
@Tag(name = "Auth", description = "Parent/Student register and login - the only 3 endpoints that don't require a token")
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email not found or wrong password - one shared error, never reveals which one it was - QUIZ_004 INVALID_CREDENTIALS")
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Username not found or wrong password - one shared error - QUIZ_004 INVALID_CREDENTIALS")
    })
    @PostMapping("/student/login")
    public ResponseEntity<ApiResponse<StudentAuthResponse>> loginStudent(@Valid @RequestBody StudentLoginRequest request) {
        return ok(authService.loginStudent(request));
    }
}
