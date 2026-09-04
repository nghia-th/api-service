package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.service.AuthService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

/**
 * "Force logout" - self-service today: a logged-in Parent/Student/Admin invalidates ALL of their
 * own sessions at once (every issued access token stops working on its very next request, not
 * just at its natural expiry; every outstanding refresh token is revoked) - see {@code
 * AuthService#logoutAll()}'s javadoc. Three thin endpoints instead of one shared path because
 * {@code JwtAuthFilter} gates by URL prefix ({@code /api/parent/*} needs a PARENT token, {@code
 * /api/student/*} needs a STUDENT token, {@code /api/admin/*} needs an ADMIN token) - {@code
 * AuthService#logoutAll()} itself reads {@code CurrentUser.get()}, so all three endpoints below
 * delegate to the exact same method; there is intentionally no fourth "any role" variant.
 * <p>
 * Not folded into {@code AuthApi} - that controller is specifically the "no token needed yet"
 * group (see its own javadoc); this one needs a valid token to know WHICH account to log out, so
 * it must sit behind {@code JwtAuthFilter} instead.
 */
@Tag(name = "Session", description = "Force-logout - invalidate every session of the current account")
@RestController
public class SessionApi extends BaseCtl {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Log out this Parent from every device/session",
            description = "Bumps the account's token version and revokes every outstanding refresh token - every currently-issued access token (this device included) stops working on its next request."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token - QUIZ_001 UNAUTHORIZED")
    })
    @PostMapping("/api/parent/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllParent() {
        authService.logoutAll();
        return ok();
    }

    @Operation(
            summary = "Log out this Student from every device/session",
            description = "Same as POST /api/parent/logout-all, for a Student-role token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token - QUIZ_001 UNAUTHORIZED")
    })
    @PostMapping("/api/student/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllStudent() {
        authService.logoutAll();
        return ok();
    }

    @Operation(
            summary = "Log out this Admin from every device/session",
            description = "Same as POST /api/parent/logout-all, for an Admin-role token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All sessions invalidated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing/invalid/expired token - QUIZ_001 UNAUTHORIZED")
    })
    @PostMapping("/api/admin/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllAdmin() {
        authService.logoutAll();
        return ok();
    }
}
