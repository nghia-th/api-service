package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response body for {@code POST /api/auth/admin/login}: access + refresh token pair (see {@link ParentAuthResponse}'s javadoc - identical reasoning) plus the safe Admin view. */
@Data
@AllArgsConstructor
public class AdminAuthResponse {
    private String accessToken;
    private String refreshToken;
    private AdminResponse admin;
}
