package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response body for {@code POST /api/auth/refresh}: a fresh access token + a rotated refresh
 * token (the one just spent is revoked in the same call - see {@code AuthService#refresh}).
 * Deliberately carries no Parent/Student profile payload (unlike {@link ParentAuthResponse}/
 * {@link StudentAuthResponse}) - the client already has that cached from its original login, and
 * this endpoint doesn't know in advance which of the two it's refreshing for. {@code role} tells
 * the caller which one it turned out to be, in case that matters (e.g. picking which localStorage
 * keys to update).
 */
@Data
@AllArgsConstructor
public class TokenPairResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
}
