package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for {@code POST /api/auth/student/login-by-id} (2026-09-06, login redesign) - the
 * final step of the new Student login flow, once the Student has picked their own name from
 * {@code POST /api/auth/student/family}'s result. {@code studentId} is the internal numeric id
 * from that lookup (never a username - the point of this whole redesign is that the Student
 * never has to type or reveal a username on a shared family device). The original username-based
 * {@code StudentLoginRequest}/{@code POST /api/auth/student/login} is UNCHANGED and still works,
 * kept as a manual fallback - see {@code AuthService#loginStudentById}.
 */
@Data
public class StudentLoginByIdRequest {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "1", description = "Student id, from a prior POST /api/auth/student/family lookup")
    private Long studentId;

    @NotBlank
    @Schema(type = "string", example = "Secret123", description = "Password")
    private String password;
}
