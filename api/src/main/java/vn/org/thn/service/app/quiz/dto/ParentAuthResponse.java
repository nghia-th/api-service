package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response body for {@code POST /api/auth/parent/register} and {@code /login}: a short-lived
 * access token (see {@code JwtUtil}) + a long-lived refresh token (see {@code AuthService}'s
 * javadoc, 2026-09-04) plus the safe Parent view. {@code refreshToken} is returned as PLAINTEXT
 * here ONLY - the server never stores it that way (see {@code entity/RefreshToken.java}), so this
 * is the one and only time the client can see it; it must hang onto it (e.g. localStorage) to call
 * {@code POST /api/auth/refresh} later.
 */
@Data
@AllArgsConstructor
public class ParentAuthResponse {
    private String accessToken;
    private String refreshToken;
    private ParentResponse parent;
}
