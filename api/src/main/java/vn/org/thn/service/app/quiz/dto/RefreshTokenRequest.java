package vn.org.thn.service.app.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for both {@code POST /api/auth/refresh} and {@code POST /api/auth/logout} - both take just the refresh token's plaintext, so one DTO covers both. */
@Data
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
