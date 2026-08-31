package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response body for {@code POST /api/auth/parent/register} and {@code /login}: a bearer token plus the safe Parent view. */
@Data
@AllArgsConstructor
public class ParentAuthResponse {
    private String token;
    private ParentResponse parent;
}
