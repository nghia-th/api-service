package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One entry in {@link StudentFamilyLookupResponse} - deliberately minimal (id + display name
 * only, no username/classroom/anything else) since this is returned to an UNAUTHENTICATED
 * caller. {@code id} is later sent back to {@code POST /api/auth/student/login-by-id} - see
 * {@code AuthService#loginStudentById}.
 */
@Data
@AllArgsConstructor
public class StudentFamilyMember {
    private Long id;
    private String fullName;
}
