package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /api/auth/student/family} (2026-09-06, login redesign) - the
 * first step of the new Student login flow: the Student types the owning Parent's identifier
 * (same email/username/phone the Parent themselves logs in with) to find "which family" this
 * device belongs to, WITHOUT typing their own username - see {@code AuthService#lookupStudentFamily}
 * for why this never reveals whether a match was found.
 */
@Data
public class StudentFamilyLookupRequest {

    @NotBlank
    @Schema(type = "string", example = "parent@example.com", description = "The owning Parent's email, username, or phone - same identifier the Parent logs in with")
    private String parentIdentifier;
}
