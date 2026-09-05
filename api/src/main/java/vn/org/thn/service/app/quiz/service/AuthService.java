package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.AdminAuthResponse;
import vn.org.thn.service.app.quiz.dto.AdminLoginRequest;
import vn.org.thn.service.app.quiz.dto.AdminResponse;
import vn.org.thn.service.app.quiz.dto.ParentAuthResponse;
import vn.org.thn.service.app.quiz.dto.ParentLoginRequest;
import vn.org.thn.service.app.quiz.dto.ParentRegisterRequest;
import vn.org.thn.service.app.quiz.dto.ParentResponse;
import vn.org.thn.service.app.quiz.dto.StudentAuthResponse;
import vn.org.thn.service.app.quiz.dto.StudentLoginRequest;
import vn.org.thn.service.app.quiz.dto.StudentResponse;
import vn.org.thn.service.app.quiz.dto.TokenPairResponse;
import vn.org.thn.service.app.quiz.entity.Admin;
import vn.org.thn.service.app.quiz.entity.Parent;
import vn.org.thn.service.app.quiz.entity.RefreshToken;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AdminRepository;
import vn.org.thn.service.app.quiz.repository.ParentRepository;
import vn.org.thn.service.app.quiz.repository.RefreshTokenRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.app.quiz.security.JwtUtil;
import vn.org.thn.service.app.quiz.security.Role;
import vn.org.thn.service.app.quiz.security.TokenVersionCache;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Auth for all 3 roles: Parent self-registration/login, Student login, Admin login (2026-09-04,
 * no self-registration - see {@code entity/Admin.java}'s javadoc), plus (2026-09-04) the
 * refresh-token/force-logout mechanism added to fix a real bug the user hit: a Parent/Student
 * account deleted (or the backend restarted) while a client still held an old token, and that
 * token kept working until its own 24h expiry - {@code JwtAuthFilter} only checked the JWT's
 * signature/expiry, never whether the account behind it still existed.
 * <p>
 * <b>The fix, in one sentence: every Parent/Student/Admin row now carries a {@code tokenVersion},
 * every access token embeds the version it was minted with, and {@code JwtAuthFilter} re-checks
 * both (row still exists + version still matches, and for Parent/Student, still active) on every
 * single request</b> - not just at login. This single mechanism covers all 3 things the user
 * asked for in the same message:
 * <ol>
 *     <li><b>The bug itself</b> - a deleted account's row lookup fails immediately, no matter how
 *     fresh the token's signature/expiry still look.</li>
 *     <li><b>Refresh tokens</b> - {@link #refresh} exchanges a still-valid refresh token (opaque
 *     random string, stored hashed in {@code refresh_token}, NOT a JWT - see {@code JwtUtil}) for
 *     a new access token, so the access token's lifetime could shrink from 24h to 60 minutes (see
 *     {@code quiz.jwt.access-token-expiration-minutes}) without forcing a real re-login every
 *     hour - the frontend does this exchange silently on a 401 (see {@code QuizApiService.ts}).</li>
 *     <li><b>Force logout</b> - {@link #invalidateSessions(Long, Role)} bumps {@code tokenVersion}
 *     (invalidating every already-issued access token on its very next request, not just at its
 *     natural expiry) and revokes every outstanding refresh token. {@link #logoutAll()} is the
 *     self-service entry point (a Parent/Student/Admin calling it on themselves via {@code POST
 *     /api/{parent,student}/logout-all}, e.g. after noticing a device they don't recognize is
 *     still logged in); {@code AdminParentService} reuses the same {@code invalidateSessions}
 *     method to force-logout a Parent (and every Student under them) the moment an Admin
 *     deactivates that Parent.</li>
 * </ol>
 * <p>
 * <b>Deactivated accounts</b> (2026-09-04, Admin feature) - {@link Parent#isActive()} gates
 * Parent login directly in {@link #loginParent}; a Student has no {@code active} flag of its own,
 * so {@link #loginStudent} checks the owning Parent's {@code active} instead (deactivating a
 * Parent locks out every one of their Students too - a Parent is the tenant boundary). {@link
 * #refresh} re-checks the same thing on every refresh call, not just at login, for the same
 * "don't let a stale credential keep working" reasoning as the tokenVersion re-check above.
 * <p>
 * Password hashing uses a plain {@link BCryptPasswordEncoder} instance, not a full Spring
 * Security context - {@code base} has no auth infra yet (see {@code
 * claude/base-module-status.md}), and pulling in all of Spring Security for one encoder class
 * would be overkill for this module's minimal-dependency style (see task 1 spec). Refresh tokens
 * use a DIFFERENT hash (SHA-256 via {@link JwtUtil#hashRefreshToken}), deliberately not BCrypt -
 * see {@code entity/RefreshToken.java}'s javadoc for why.
 * <p>
 * Blank-field checks are no longer done here - {@code @Valid} + {@code @NotBlank}/{@code
 * @Email}/{@code @Size} on the request DTOs (enforced in {@code AuthApi}) reject those before
 * this class's methods ever run, via {@code base}'s {@code GlobalExceptionHandler}.
 */
@Service
public class AuthService extends IBase {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenVersionCache tokenVersionCache;

    @Value("${quiz.jwt.refresh-token-expiration-days:30}")
    private long refreshTokenExpirationDays;

    /** Registers a new Parent and auto-logs them in (returns a token pair immediately - simpler than requiring a separate login call right after register). */
    public ParentAuthResponse registerParent(ParentRegisterRequest request) {
        if (parentRepository.query().eq(Parent::getEmail, request.getEmail()).exists()) {
            throw new BusinessException(QuizErrorCode.EMAIL_TAKEN);
        }
        // Optional (2026-09-05) - a self-registering Parent may set a username right away, or
        // leave it blank and set one later via POST /api/parent/set-username (see #setUsername).
        String username = normalizeUsername(request.getUsername());
        if (username != null && parentRepository.query().eq(Parent::getUsername, username).exists()) {
            throw new BusinessException(QuizErrorCode.USERNAME_TAKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        Parent parent = new Parent();
        parent.setFullName(request.getFullName());
        parent.setEmail(request.getEmail());
        parent.setPassword(passwordEncoder.encode(request.getPassword()));
        parent.setPhone(request.getPhone());
        parent.setUsername(username);
        parent.setCreatedAt(now);
        parent.setUpdatedAt(now);
        parent.setCreatedBy(request.getEmail());
        parent.setUpdatedBy(request.getEmail());
        parent = parentRepository.save(parent);

        logInfo("Parent registered: id={}, email={}", parent.getId(), parent.getEmail());
        String accessToken = jwtUtil.generate(parent.getId(), Role.PARENT, parent.getTokenVersion());
        String refreshToken = issueRefreshToken(parent.getId(), Role.PARENT);
        return new ParentAuthResponse(accessToken, refreshToken, ParentResponse.from(parent));
    }

    public ParentAuthResponse loginParent(ParentLoginRequest request) {
        // Tries email, username, then phone (2026-09-05) - see ParentLoginRequest#identifier's
        // javadoc. orEq() is OR-joined with the eq() before it, so this renders a single
        // "email = :id OR username = :id OR phone = :id" query, not 3 separate lookups.
        Parent parent = parentRepository.query()
                .eq(Parent::getEmail, request.getIdentifier())
                .orEq(Parent::getUsername, request.getIdentifier())
                .orEq(Parent::getPhone, request.getIdentifier())
                .one();
        // Same error for "no matching identifier" and "wrong password" - never reveal which one it was (see task 1 spec).
        if (parent == null || !passwordEncoder.matches(request.getPassword(), parent.getPassword())) {
            throw new BusinessException(QuizErrorCode.INVALID_CREDENTIALS);
        }
        // Checked AFTER the password match on purpose - an unauthenticated caller must not learn
        // "this email exists but is deactivated" before proving they know the password.
        if (!parent.isActive()) {
            throw new BusinessException(QuizErrorCode.ACCOUNT_DEACTIVATED);
        }
        String accessToken = jwtUtil.generate(parent.getId(), Role.PARENT, parent.getTokenVersion());
        String refreshToken = issueRefreshToken(parent.getId(), Role.PARENT);
        return new ParentAuthResponse(accessToken, refreshToken, ParentResponse.from(parent));
    }

    public StudentAuthResponse loginStudent(StudentLoginRequest request) {
        Student student = studentRepository.query().eq(Student::getUsername, request.getUsername()).one();
        if (student == null || !passwordEncoder.matches(request.getPassword(), student.getPassword())) {
            throw new BusinessException(QuizErrorCode.INVALID_CREDENTIALS);
        }
        // A Student has no active flag of its own - it inherits the owning Parent's, since a
        // Parent is the tenant boundary (deactivating a Parent locks out their Students too).
        Parent owningParent = parentRepository.findById(student.getParentId());
        if (owningParent == null || !owningParent.isActive()) {
            throw new BusinessException(QuizErrorCode.ACCOUNT_DEACTIVATED);
        }
        String accessToken = jwtUtil.generate(student.getId(), Role.STUDENT, student.getTokenVersion());
        String refreshToken = issueRefreshToken(student.getId(), Role.STUDENT);
        return new StudentAuthResponse(accessToken, refreshToken, StudentResponse.from(student));
    }

    /** Admin login - no self-registration, see {@code entity/Admin.java}'s javadoc for how the first Admin row is created. */
    public AdminAuthResponse loginAdmin(AdminLoginRequest request) {
        // Tries email, username, then phone (2026-09-05) - see AdminLoginRequest#identifier's
        // javadoc, same OR-query shape as #loginParent above.
        Admin admin = adminRepository.query()
                .eq(Admin::getEmail, request.getIdentifier())
                .orEq(Admin::getUsername, request.getIdentifier())
                .orEq(Admin::getPhone, request.getIdentifier())
                .one();
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(QuizErrorCode.INVALID_CREDENTIALS);
        }
        String accessToken = jwtUtil.generate(admin.getId(), Role.ADMIN, admin.getTokenVersion());
        String refreshToken = issueRefreshToken(admin.getId(), Role.ADMIN);
        return new AdminAuthResponse(accessToken, refreshToken, AdminResponse.from(admin));
    }

    /**
     * Exchanges a still-valid refresh token for a new access token, rotating the refresh token in
     * the same call (the one just spent is revoked, a brand-new one issued and returned) -
     * standard refresh-token-rotation practice, so a stolen-but-unused old refresh token stops
     * working the moment the legitimate client rotates past it.
     * <p>
     * Re-checks the owning Parent/Student/Admin still exists (and, for Parent/Student, is still
     * active) on every call (not just at login) - this is what makes a force-logout ({@link
     * #logoutAll()}), an account deletion, or a deactivation actually take effect for a client
     * that keeps calling this with an old refresh token, same reasoning as {@code JwtAuthFilter}
     * re-checking tokenVersion/active on every request.
     */
    public TokenPairResponse refresh(String refreshTokenPlaintext) {
        RefreshToken row = findValidRefreshTokenOrThrow(refreshTokenPlaintext);
        LocalDateTime now = LocalDateTime.now();

        row.setRevoked(true);
        row.setUpdatedAt(now);
        refreshTokenRepository.save(row);

        Role role = Role.valueOf(row.getRole());
        int tokenVersion;
        if (role == Role.PARENT) {
            Parent parent = parentRepository.findById(row.getUserId());
            if (parent == null || !parent.isActive()) {
                throw new BusinessException(QuizErrorCode.REFRESH_TOKEN_INVALID);
            }
            tokenVersion = parent.getTokenVersion();
        } else if (role == Role.STUDENT) {
            Student student = studentRepository.findById(row.getUserId());
            if (student == null) {
                throw new BusinessException(QuizErrorCode.REFRESH_TOKEN_INVALID);
            }
            Parent owningParent = parentRepository.findById(student.getParentId());
            if (owningParent == null || !owningParent.isActive()) {
                throw new BusinessException(QuizErrorCode.REFRESH_TOKEN_INVALID);
            }
            tokenVersion = student.getTokenVersion();
        } else {
            Admin admin = adminRepository.findById(row.getUserId());
            if (admin == null) {
                throw new BusinessException(QuizErrorCode.REFRESH_TOKEN_INVALID);
            }
            tokenVersion = admin.getTokenVersion();
        }

        String accessToken = jwtUtil.generate(row.getUserId(), role, tokenVersion);
        String newRefreshToken = issueRefreshToken(row.getUserId(), role);
        logInfo("Access token refreshed: userId={}, role={}", row.getUserId(), role);
        return new TokenPairResponse(accessToken, newRefreshToken, role.name());
    }

    /**
     * Logs out one device/session: revokes the given refresh token if it exists - silently no-ops
     * otherwise (already revoked/expired/unknown token - never reveals which, same "don't leak"
     * reasoning as {@link #loginParent}). Does NOT touch {@code tokenVersion}, so this device's
     * still-live access token (if any) keeps working until its own short expiry - use {@link
     * #logoutAll()} to invalidate those immediately too.
     */
    public void logout(String refreshTokenPlaintext) {
        String hash = jwtUtil.hashRefreshToken(refreshTokenPlaintext);
        RefreshToken row = refreshTokenRepository.query().eq(RefreshToken::getTokenHash, hash).one();
        if (row != null && !row.isRevoked()) {
            row.setRevoked(true);
            row.setUpdatedAt(LocalDateTime.now());
            refreshTokenRepository.save(row);
        }
    }

    /**
     * "Force logout" - invalidates EVERY session of the CURRENT caller ({@link CurrentUser#get()})
     * at once. Thin self-service wrapper over {@link #invalidateSessions(Long, Role)} - see that
     * method's javadoc for the actual mechanism and its other caller (Admin deactivating a
     * Parent).
     */
    public void logoutAll() {
        CurrentUser current = CurrentUser.get();
        invalidateSessions(current.userId(), current.role());
    }

    /**
     * Self-service change-password for the CURRENT caller ({@link CurrentUser#get()}), any of the
     * 3 roles (2026-09-04, per the user's explicit request). Verifies {@code oldPassword} against
     * the stored hash first (see {@code ChangePasswordRequest}'s javadoc for why this is required
     * even though the caller is already authenticated) - a mismatch throws {@link
     * QuizErrorCode#OLD_PASSWORD_INCORRECT} and nothing is changed. On success, hashes and saves
     * {@code newPassword}, then calls {@link #invalidateSessions(Long, Role)} on the caller's own
     * account - deliberately including the CURRENT session, standard "password changed, log in
     * again everywhere" practice; the frontend must treat a successful response the same as a
     * manual logout (clear tokens, redirect to /login), since this call's own access token stops
     * working on its very next request.
     */
    public void changePassword(String oldPassword, String newPassword) {
        CurrentUser current = CurrentUser.get();
        Long userId = current.userId();
        Role role = current.role();
        LocalDateTime now = LocalDateTime.now();

        if (role == Role.PARENT) {
            Parent parent = parentRepository.findById(userId);
            if (parent == null || !passwordEncoder.matches(oldPassword, parent.getPassword())) {
                throw new BusinessException(QuizErrorCode.OLD_PASSWORD_INCORRECT);
            }
            parent.setPassword(passwordEncoder.encode(newPassword));
            parent.setUpdatedAt(now);
            parentRepository.save(parent);
        } else if (role == Role.STUDENT) {
            Student student = studentRepository.findById(userId);
            if (student == null || !passwordEncoder.matches(oldPassword, student.getPassword())) {
                throw new BusinessException(QuizErrorCode.OLD_PASSWORD_INCORRECT);
            }
            student.setPassword(passwordEncoder.encode(newPassword));
            student.setUpdatedAt(now);
            studentRepository.save(student);
        } else {
            Admin admin = adminRepository.findById(userId);
            if (admin == null || !passwordEncoder.matches(oldPassword, admin.getPassword())) {
                throw new BusinessException(QuizErrorCode.OLD_PASSWORD_INCORRECT);
            }
            admin.setPassword(passwordEncoder.encode(newPassword));
            admin.setUpdatedAt(now);
            adminRepository.save(admin);
        }

        invalidateSessions(userId, role);
        logInfo("Password changed (self-service): userId={}, role={}", userId, role);
    }

    /**
     * Self-service "set/change username" for the CURRENT caller ({@link CurrentUser#get()}) -
     * Parent or Admin only (2026-09-05, per the user's explicit request that both roles support
     * logging in by email/username/phone). Not offered to Student, which already always has a
     * username set at creation time. Unlike {@link #changePassword}, this does NOT force-logout -
     * a username is not a secret, so an in-flight session isn't compromised by someone else
     * learning it, unlike a password change.
     */
    public void setUsername(String username) {
        CurrentUser current = CurrentUser.get();
        Role role = current.role();
        Long userId = current.userId();
        String normalized = normalizeUsername(username);

        if (role == Role.PARENT) {
            if (normalized != null && parentRepository.query().eq(Parent::getUsername, normalized).ne(Parent::getId, userId).exists()) {
                throw new BusinessException(QuizErrorCode.USERNAME_TAKEN);
            }
            Parent parent = parentRepository.findById(userId);
            if (parent != null) {
                parent.setUsername(normalized);
                parent.setUpdatedAt(LocalDateTime.now());
                parentRepository.save(parent);
            }
        } else if (role == Role.ADMIN) {
            if (normalized != null && adminRepository.query().eq(Admin::getUsername, normalized).ne(Admin::getId, userId).exists()) {
                throw new BusinessException(QuizErrorCode.USERNAME_TAKEN);
            }
            Admin admin = adminRepository.findById(userId);
            if (admin != null) {
                admin.setUsername(normalized);
                admin.setUpdatedAt(LocalDateTime.now());
                adminRepository.save(admin);
            }
        } else {
            // Student already always has a username - nothing to do, but no error either;
            // AccountApi simply doesn't expose this endpoint under /api/student/**.
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        logInfo("Username set: userId={}, role={}", userId, role);
    }

    /** Trims blank/whitespace-only input to null (treated as "leave unset"), otherwise returns the trimmed value - shared by {@link #registerParent} and {@link #setUsername}. */
    private String normalizeUsername(String username) {
        if (username == null) return null;
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Invalidates EVERY session of the given {@code userId}/{@code role} at once: bumps {@code
     * tokenVersion} (so every already-issued access token fails its next {@code JwtAuthFilter}
     * check immediately, not just at its natural expiry) and revokes every outstanding refresh
     * token. Shared by {@link #logoutAll()} (a Parent/Student/Admin force-logging-out themselves)
     * and {@code AdminParentService} (an Admin force-logging-out a Parent it just deactivated, and
     * every Student under that Parent - see this class's javadoc, "Deactivated accounts").
     */
    public void invalidateSessions(Long userId, Role role) {
        LocalDateTime now = LocalDateTime.now();

        if (role == Role.PARENT) {
            Parent parent = parentRepository.findById(userId);
            if (parent != null) {
                parent.setTokenVersion(parent.getTokenVersion() + 1);
                parent.setUpdatedAt(now);
                parentRepository.save(parent);
            }
        } else if (role == Role.STUDENT) {
            Student student = studentRepository.findById(userId);
            if (student != null) {
                student.setTokenVersion(student.getTokenVersion() + 1);
                student.setUpdatedAt(now);
                studentRepository.save(student);
            }
        } else {
            Admin admin = adminRepository.findById(userId);
            if (admin != null) {
                admin.setTokenVersion(admin.getTokenVersion() + 1);
                admin.setUpdatedAt(now);
                adminRepository.save(admin);
            }
        }

        List<RefreshToken> liveTokens = refreshTokenRepository.query()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getRole, role.name())
                .eq(RefreshToken::isRevoked, false)
                .list();
        for (RefreshToken row : liveTokens) {
            row.setRevoked(true);
            row.setUpdatedAt(now);
            refreshTokenRepository.save(row);
        }

        // Evict the cached tokenVersion/active state (2026-09-04, RAM cache) - the tokenVersion
        // bump above is worthless if a stale cache entry keeps letting the OLD tokenVersion pass
        // JwtAuthFilter's check. See TokenVersionCache's javadoc, "EVICT on every mutation" - this
        // is the single choke point every caller of this method (self-service logoutAll, Admin
        // deactivating a Parent, and 2026-09-04's change-password/reset-password, which all call
        // this method) relies on for that guarantee.
        tokenVersionCache.evict(role, userId);

        logInfo("Sessions invalidated: userId={}, role={}, revokedRefreshTokens={}",
                userId, role, liveTokens.size());
    }

    /** Issues + persists (hashed) a brand-new refresh token for {@code userId}/{@code role}, returning its ONE-TIME plaintext - see {@link ParentAuthResponse}'s javadoc for why this is safe to return but never re-visible afterward. */
    private String issueRefreshToken(Long userId, Role role) {
        String plaintext = jwtUtil.newRefreshTokenPlaintext();
        LocalDateTime now = LocalDateTime.now();
        String actor = role.name().toLowerCase() + ":" + userId;

        RefreshToken row = new RefreshToken();
        row.setUserId(userId);
        row.setRole(role.name());
        row.setTokenHash(jwtUtil.hashRefreshToken(plaintext));
        row.setExpiresAt(now.plusDays(refreshTokenExpirationDays));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setCreatedBy(actor);
        row.setUpdatedBy(actor);
        refreshTokenRepository.save(row);

        return plaintext;
    }

    /** Loads the DB row for a refresh token's plaintext, throwing {@link QuizErrorCode#REFRESH_TOKEN_INVALID} if it doesn't exist, is already revoked, or has expired - one shared error for all 3 cases, same "don't leak which" reasoning as {@link QuizErrorCode#INVALID_CREDENTIALS}. */
    private RefreshToken findValidRefreshTokenOrThrow(String refreshTokenPlaintext) {
        String hash = jwtUtil.hashRefreshToken(refreshTokenPlaintext);
        RefreshToken row = refreshTokenRepository.query().eq(RefreshToken::getTokenHash, hash).one();
        if (row == null || row.isRevoked() || row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(QuizErrorCode.REFRESH_TOKEN_INVALID);
        }
        return row;
    }
}
