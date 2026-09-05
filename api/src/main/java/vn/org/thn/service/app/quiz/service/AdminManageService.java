package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.org.thn.service.app.quiz.dto.AdminCreateRequest;
import vn.org.thn.service.app.quiz.dto.AdminSummary;
import vn.org.thn.service.app.quiz.entity.Admin;
import vn.org.thn.service.app.quiz.entity.RefreshToken;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AdminRepository;
import vn.org.thn.service.app.quiz.repository.RefreshTokenRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.app.quiz.security.Role;
import vn.org.thn.service.app.quiz.security.TokenVersionCache;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin-manages-Admin (2026-09-05, per the user's explicit request: "tài khoản root la tài khoản
 * cao nhất chỉ có quyền xoá các admin khác, các admin khác k được xoá root") - list every Admin,
 * create a new (always non-root) one, and delete one. Same overall shape as {@link
 * AdminParentService} (an Admin's management of a DIFFERENT role) but the ownership rule here is
 * unlike anything else in this codebase: there is no "owns" relationship between Admin rows at
 * all, so instead of an ownership check this uses a RANK check - every method here is root-only
 * (see {@link #requireRoot()}), i.e. only the single bootstrap {@code root=true} account (see
 * {@code entity/Admin.java}'s javadoc) can call any of them; a regular (non-root) Admin's token is
 * still perfectly valid for every OTHER {@code /api/admin/**} endpoint (Parent management,
 * translations, self-service change-password), it simply gets {@link CommonErrorCode#FORBIDDEN}
 * on this one family of endpoints - enforced here in the Service layer (not a URL-prefix rule
 * {@code SecurityConfig}/{@code JwtAuthFilter} could express, since both root and non-root Admins
 * share the exact same {@code /api/admin/*} prefix and token shape).
 * <p>
 * <b>{@link #delete}</b> additionally refuses to remove a row where {@link Admin#isRoot()} is true
 * ({@link QuizErrorCode#ROOT_ADMIN_CANNOT_BE_DELETED}) - REGARDLESS of who calls it. In practice
 * the only caller that can ever reach this method at all is root itself (see the rank check
 * above), so this second check's only real effect is stopping root from deleting ITSELF (there is
 * no other Admin with {@code root=true} to accidentally hit) - kept as its own explicit check
 * rather than folded into {@link #requireRoot} because it is a different rule about a different
 * row (the TARGET, not the caller) and deserves its own error code so the two failure reasons are
 * never confused in a log or a frontend message.
 */
@Service
public class AdminManageService extends IBase {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenVersionCache tokenVersionCache;

    /** Every Admin in the system - same "no ORDER BY, client sorts/filters" convention as {@code AdminParentService#list}. Root-only, see this class's javadoc. */
    public List<AdminSummary> list() {
        requireRoot();
        return adminRepository.findAll().stream().map(AdminSummary::from).toList();
    }

    /** Creates a new, always non-root, Admin account. Root-only, see this class's javadoc. */
    @Transactional
    public AdminSummary create(AdminCreateRequest request) {
        Long callerId = requireRoot();

        if (adminRepository.query().eq(Admin::getEmail, request.getEmail()).exists()) {
            throw new BusinessException(QuizErrorCode.EMAIL_TAKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        String actor = "admin:" + callerId;

        Admin admin = new Admin();
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRoot(false);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setCreatedBy(actor);
        admin.setUpdatedBy(actor);
        admin = adminRepository.save(admin);

        logInfo("Admin created by root: id={}, email={}, rootId={}", admin.getId(), admin.getEmail(), callerId);
        return AdminSummary.from(admin);
    }

    /**
     * Permanently deletes Admin {@code id}. Root-only (see this class's javadoc), and additionally
     * refuses when the TARGET row is itself the root account ({@link
     * QuizErrorCode#ROOT_ADMIN_CANNOT_BE_DELETED}) - see this class's javadoc for why that check is
     * separate from {@link #requireRoot}. Force-logs-out the deleted Admin's own sessions (same
     * {@code invalidateSessions}-shaped cleanup as {@code AdminParentService#deleteCascade}, just
     * for one row with nothing owned underneath it to cascade to - an Admin owns no Parent/Student
     * data of its own).
     */
    @Transactional
    public void delete(Long id) {
        Long callerId = requireRoot();

        Admin target = adminRepository.findById(id);
        if (target == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Admin not found");
        }
        if (target.isRoot()) {
            throw new BusinessException(QuizErrorCode.ROOT_ADMIN_CANNOT_BE_DELETED);
        }

        adminRepository.deleteById(id);
        refreshTokenRepository.delete().eq(RefreshToken::getUserId, id).eq(RefreshToken::getRole, Role.ADMIN.name()).execute();
        // Evict cache for the deleted Admin (2026-09-05, RAM cache) - same reasoning as every other
        // delete path in this codebase, see TokenVersionCache's javadoc: a stale cache entry for a
        // row that no longer exists would otherwise keep letting an already-issued token pass
        // JwtAuthFilter's check until something else happens to evict this entry.
        tokenVersionCache.evict(Role.ADMIN, id);

        logInfo("Admin deleted by root: id={}, rootId={}", id, callerId);
    }

    /**
     * Loads the CURRENT caller's own Admin row and throws {@link CommonErrorCode#FORBIDDEN} unless
     * it is the root account - the single rank check every method in this class starts with. Never
     * trusts a client-supplied flag: always re-reads {@link Admin#isRoot()} fresh from the DB via
     * {@link CurrentUser#get()}'s userId, the same "never trust the caller, only the token's userId"
     * convention every ownership check in this codebase already follows.
     *
     * @return the caller's own Admin id, for {@code createdBy}/log lines.
     */
    private Long requireRoot() {
        Long callerId = CurrentUser.get().userId();
        Admin caller = adminRepository.findById(callerId);
        if (caller == null || !caller.isRoot()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Only the root admin can manage other Admin accounts");
        }
        return callerId;
    }
}
