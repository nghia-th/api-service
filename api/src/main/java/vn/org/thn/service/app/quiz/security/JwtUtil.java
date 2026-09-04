package vn.org.thn.service.app.quiz.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

/**
 * Generates/verifies the access JWTs used across both roles (Parent/Student), plus the plain
 * (non-JWT) helpers {@code AuthService} uses for refresh tokens - opaque random strings, hashed
 * before storage (see {@link #newRefreshTokenPlaintext()}/{@link #hashRefreshToken(String)}).
 * {@code base} has no auth infra yet (see {@code claude/base-module-status.md}), so this is new,
 * quiz-service-only code, not something reused from {@code base}.
 * <p>
 * Uses jjwt's fluent 0.12+/0.13 API ({@code Jwts.builder()...signWith()}, {@code
 * Jwts.parser().verifyWith()...}) with the {@code jjwt-gson} runtime implementation, deliberately
 * NOT {@code jjwt-jackson}: {@code base} is already on Jackson 3 ({@code tools.jackson.*}, see
 * {@link vn.org.thn.service.base.util.JsonUtils}), while jjwt's Jackson support still targets
 * Jackson 2 ({@code com.fasterxml.jackson.databind}) - pulling that in would add a second,
 * unrelated JSON stack just for token (de)serialization. Gson has no such conflict and jjwt's
 * payload here is a tiny flat claims map, so it needs nothing from Jackson's feature set anyway.
 * <p>
 * <b>tokenVersion (2026-09-04, fixes the "deleted account's old token still works" bug):</b> the
 * access token now carries a {@code tv} (tokenVersion) claim alongside {@code role}, checked by
 * {@link JwtAuthFilter} against the CURRENT value on the Parent/Student row on every request -
 * a token whose {@code tv} no longer matches (bumped by {@code AuthService#logoutAll()}) or whose
 * owning row no longer exists at all (deleted) is rejected outright, even though the JWT itself
 * is still validly signed and unexpired. This is the mechanism behind both the bug fix and
 * "force logout" - see {@code AuthService}'s javadoc.
 * <p>
 * <b>Refresh tokens are deliberately NOT JWTs</b> - a refresh token has no payload worth signing
 * (it is looked up by its hash in the {@code refresh_token} table, which is also where its
 * expiry/revoked state actually lives), so a plain cryptographically-random string is simpler and
 * needs no separate signing key/claims shape. {@link #accessTokenExpiration} shrank from the
 * pre-refresh-token default of 24h to a short-lived 60 minutes (see {@code
 * quiz.jwt.access-token-expiration-minutes} in {@code application.yaml}) - now that the frontend
 * silently exchanges a refresh token for a new access token on 401 (see {@code
 * QuizApiService.ts}), a short access-token lifetime costs nothing in UX but shrinks the window a
 * stolen access token stays useful.
 */
@Component
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_VERSION = "tv";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey signingKey;
    private final Duration accessTokenExpiration;

    public JwtUtil(
            @Value("${quiz.jwt.secret}") String secret,
            @Value("${quiz.jwt.access-token-expiration-minutes:60}") long accessTokenExpirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = Duration.ofMinutes(accessTokenExpirationMinutes);
    }

    /** Issues a signed access token for {@code userId}/{@code role}/{@code tokenVersion}, valid for {@code quiz.jwt.access-token-expiration-minutes} from now. */
    public String generate(Long userId, Role role, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
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
        int tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        return new Payload(userId, role, tokenVersion);
    }

    /** A fresh, cryptographically random refresh token plaintext (48 random bytes, URL-safe base64, no padding) - NOT a JWT, see class javadoc. Callers must hash it with {@link #hashRefreshToken} before persisting, and return only this plaintext to the client. */
    public String newRefreshTokenPlaintext() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hash of a refresh token's plaintext, hex-encoded - what actually gets stored in/looked up against the {@code refresh_token} table, so a DB leak alone never yields a usable token. */
    public String hashRefreshToken(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Every JVM ships SHA-256 (JCA standard algorithm) - unreachable in practice.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Decoded access token payload: which user, which role, which tokenVersion it was minted with. */
    public record Payload(Long userId, Role role, int tokenVersion) {
    }
}
