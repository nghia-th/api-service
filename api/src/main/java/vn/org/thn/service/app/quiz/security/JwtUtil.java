package vn.org.thn.service.app.quiz.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Generates and verifies the JWTs used across both roles (Parent/Student). {@code base} has no
 * auth infra yet (see {@code claude/base-module-status.md}), so this is new, quiz-service-only
 * code, not something reused from {@code base}.
 * <p>
 * Uses jjwt's fluent 0.12+/0.13 API ({@code Jwts.builder()...signWith()}, {@code
 * Jwts.parser().verifyWith()...}) with the {@code jjwt-gson} runtime implementation, deliberately
 * NOT {@code jjwt-jackson}: {@code base} is already on Jackson 3 ({@code tools.jackson.*}, see
 * {@link vn.org.thn.service.base.util.JsonUtils}), while jjwt's Jackson support still targets
 * Jackson 2 ({@code com.fasterxml.jackson.databind}) - pulling that in would add a second,
 * unrelated JSON stack just for token (de)serialization. Gson has no such conflict and jjwt's
 * payload here is a tiny flat claims map, so it needs nothing from Jackson's feature set anyway.
 */
@Component
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtUtil(
            @Value("${quiz.jwt.secret}") String secret,
            @Value("${quiz.jwt.expiration-minutes:1440}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    /** Issues a signed token for {@code userId}/{@code role}, valid for {@code quiz.jwt.expiration-minutes} from now. */
    public String generate(Long userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the signature and expiry of {@code token} and extracts its payload.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException if the token's expiry has passed
     * @throws io.jsonwebtoken.JwtException        for any other invalid/malformed/tampered token
     */
    public Payload parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new Payload(userId, role);
    }

    /** Decoded token payload: which user, which role. */
    public record Payload(Long userId, Role role) {
    }
}
