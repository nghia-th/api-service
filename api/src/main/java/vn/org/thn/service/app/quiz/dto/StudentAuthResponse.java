package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response body for {@code POST /api/auth/student/login}: an access token + refresh token pair (see {@link ParentAuthResponse}'s javadoc - identical reasoning, just for Student) plus the safe Student view. */
@Data
@AllArgsConstructor
public class StudentAuthResponse {
    private String accessToken;
    private String refreshToken;
    private StudentResponse student;
}
