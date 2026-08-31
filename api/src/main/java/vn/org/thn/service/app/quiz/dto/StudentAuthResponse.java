package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response body for {@code POST /api/auth/student/login}: a bearer token plus the safe Student view. */
@Data
@AllArgsConstructor
public class StudentAuthResponse {
    private String token;
    private StudentResponse student;
}
