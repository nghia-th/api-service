package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;
import vn.org.thn.service.base.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * A refresh token issued to a Parent or Student, stored HASHED ({@code tokenHash}, SHA-256 via
 * {@link vn.org.thn.service.app.quiz.security.JwtUtil#hashRefreshToken}) - the plaintext is
 * returned to the client exactly once (at issue time, see {@code AuthService#issueRefreshToken})
 * and never persisted, so a DB leak alone never yields a usable token (same reasoning as {@code
 * password} on {@link Parent}/{@link Student}, just a different hash algorithm - BCrypt is
 * deliberately NOT used here since a refresh token is already high-entropy random data, not a
 * human-chosen password that needs slow hashing against brute force).
 * <p>
 * {@code role} is stored as the plain {@code Role} enum name (String) rather than a foreign key,
 * mirroring how {@code JwtUtil} carries it as a JWT claim - {@code userId} alone is not unique
 * across the two tables ({@code parent}/{@code student} each have their own auto-increment
 * sequence), so {@code role} disambiguates which table {@code userId} refers to.
 * <p>
 * {@code revoked} is the real-world "logged out" flag for this one refresh token (rotated away on
 * every {@code POST /api/auth/refresh} call, or set directly by {@code POST /api/auth/logout} and
 * {@code POST /api/{parent,student}/logout-all}) - kept separate from {@code deleted} (inherited
 * from {@link BaseEntity}, unused here, kept only for consistency with every other entity in this
 * service extending it).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String role;

    @ToString.Exclude
    private String tokenHash;

    private LocalDateTime expiresAt;
    private boolean revoked = false;
}
