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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.AdminCreateRequest;
import vn.org.thn.service.app.quiz.dto.AdminSummary;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.AdminManageService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Admin-manages-Admin (2026-09-05): list/create/delete OTHER Admin accounts - "root la tai khoan
 * cao nhat, chi root xoa duoc admin khac, cac admin khac khong duoc xoa root" (the user's explicit
 * request). Every endpoint here sits behind {@link JwtAuthFilter} under {@code /api/admin/*} same
 * as {@link AdminParentApi} (any ADMIN-role token can reach the URL), but EVERY method additionally
 * requires the caller to be the root account specifically - see {@link
 * AdminManageService#requireRoot}'s javadoc for why that extra rank check has to live in the
 * Service layer rather than {@code SecurityConfig}'s URL-prefix rule.
 */
@Tag(name = "Admin - Admins", description = "Root-only management of other Admin accounts")
@RestController
@RequestMapping("/api/admin/admins")
public class AdminManageApi extends BaseCtl {

    @Autowired
    private AdminManageService adminManageService;

    @Operation(
            summary = "List every Admin account",
            description = "Root-only - a non-root Admin token gets 403 COMMON_004 FORBIDDEN. No pagination in v1, same as every other 'list everything' endpoint in this app."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every Admin, each with root/createdAt"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not the root admin - COMMON_004 FORBIDDEN")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSummary>>> list() {
        return ok(adminManageService.list());
    }

    @Operation(
            summary = "Create a new Admin account",
            description = "Root-only. The new account is always a regular (non-root) Admin - there is no way to create a second root account through this endpoint."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created - returns the new Admin's info (password excluded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field (fullName/email/password) is missing or malformed - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not the root admin - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email is already registered to another Admin - QUIZ_002 EMAIL_TAKEN")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AdminSummary>> create(@Valid @RequestBody AdminCreateRequest request) {
        return ok(adminManageService.create(request));
    }

    @Operation(
            summary = "Delete an Admin account",
            description = "Root-only, and the root account itself can never be deleted through this endpoint (by anyone, including itself) - see AdminManageService#delete's javadoc."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not the root admin - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No Admin with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "id is the root account - cannot be deleted - QUIZ_031 ROOT_ADMIN_CANNOT_BE_DELETED")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Admin id") @PathVariable Long id) {
        adminManageService.delete(id);
        return ok();
    }
}
