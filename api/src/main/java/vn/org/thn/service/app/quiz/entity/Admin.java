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

/**
 * An administrator account (2026-09-04) - manages Parent accounts (list/create/activate-
 * deactivate/reset-password/delete, see {@code AdminParentApi}). Since 2026-09-05, Admins also
 * manage OTHER Admin accounts (list/create/delete, see {@code AdminManageApi}) - but that is
 * root-only (see {@code root} below), so a regular (non-root) Admin is still a single flat role
 * with no further permission tiers of its own, same as {@link Parent}/{@link Student} each being
 * their own flat role.
 * <p>
 * <b>No self-registration</b> - unlike Parent, there is no {@code POST /api/auth/admin/register}.
 * The first (and, in v1, only) Admin row is created at startup by {@code
 * config/AdminBootstrapRunner} - a FIXED {@code root}/{@code root} login (2026-09-04, replacing
 * the earlier {@code quiz.admin.bootstrap-email}/{@code bootstrap-password} application.yaml
 * config - see that class's javadoc for why it changed) if the {@code admin} table is empty - see
 * that class's javadoc for why bootstrapping is needed at all (nothing/no-one exists yet to call
 * a "create admin" endpoint with).
 * <p>
 * <b>{@code root} (2026-09-04):</b> true ONLY for that one bootstrap-created account - marks it
 * as the highest-ranking Admin, per the user's explicit request ("root la tai khoan cao nhat").
 * Enforced two ways since 2026-09-05's Admin-manages-Admin feature ({@code AdminManageService}):
 * (1) every {@code /api/admin/admins/**} endpoint (list/create/delete OTHER Admin accounts) is
 * root-only - a non-root Admin's token gets {@code CommonErrorCode.FORBIDDEN}, see {@code
 * AdminManageService#requireRoot}; (2) a row where {@code root=true} can never be deleted through
 * that endpoint EITHER, not even by itself - see {@code AdminManageService#delete} and {@code
 * QuizErrorCode#ROOT_ADMIN_CANNOT_BE_DELETED}. This account's PASSWORD can still be changed (see
 * 2026-09-04's change-password feature) - only deletion is blocked by this flag.
 * <p>
 * {@code tokenVersion} - same purpose/mechanism as {@link Parent#getTokenVersion()}/{@link
 * Student#getTokenVersion()} (force-logout, see {@code AuthService}'s javadoc) - included here
 * too so an Admin session can also be force-logged-out consistently (now also triggered by a
 * self-service password change, see {@code AuthService#changePassword}).
 * <p>
 * {@code username}/{@code phone} (2026-09-05) - OPTIONAL alternate login identifiers, per the
 * user's explicit request that both Admin and Parent be able to log in with email, username, OR
 * phone (not just email). Both are nullable with NO backfill for existing rows - an Admin created
 * before this feature simply has neither until it calls the new self-service "set username"
 * endpoint ({@code AccountApi}/{@code AuthService#setUsername}); {@code phone} for now can only be
 * set at creation time ({@code AdminManageService#create}), same as Parent's {@code phone}. Each
 * is unique within the {@code admin} table only (see the {@code uq_admin_username} constraint) -
 * NOT cross-checked against Parent/Student, same as {@code email} already isn't. See {@code
 * AuthService#loginAdmin} for the actual email-OR-username-OR-phone lookup.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "admin")
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @ToString.Exclude
    private String password;

    private String fullName;
    private int tokenVersion = 0;
    private boolean root = false;
    private String username;
    private String phone;
}
