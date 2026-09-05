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
 * A parent account - the top-level tenant boundary in quiz-service: every Subject/Lesson/
 * Question/Test a parent owns must be filtered by {@code parentId} in every {@code *Service}
 * method (see {@code docs/01-thiet-ke-tong-the.md} section 3, auth).
 * <p>
 * {@code password} is a BCrypt hash, never the plaintext. {@code @ToString.Exclude} keeps it out
 * of log lines even by accident (e.g. an {@code IBase.logInfo(entity)} call), on top of the
 * existing rule that no API response ever returns this field directly - see {@link
 * vn.org.thn.service.app.quiz.dto.ParentResponse}.
 * <p>
 * {@code @EqualsAndHashCode(callSuper = true)}/{@code @ToString(callSuper = true)} are required
 * because {@link BaseEntity} also carries its own {@code @Data} - without {@code callSuper}, the
 * generated {@code equals}/{@code hashCode}/{@code toString} would silently ignore the 5 inherited
 * audit fields (same reasoning as {@code Article} in {@code api-service}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "parent")
public class Parent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;

    @ToString.Exclude
    private String password;

    private String phone;

    /**
     * OPTIONAL alternate login identifier (2026-09-05), alongside {@code email}/{@code phone} -
     * per the user's explicit request that Parent (and Admin) be able to log in with email,
     * username, OR phone. Nullable with NO backfill for existing rows - a Parent registered before
     * this feature simply has none until it calls the new self-service "set username" endpoint
     * ({@code AccountApi}/{@code AuthService#setUsername}). Unique within the {@code parent} table
     * only (see the {@code uq_parent_username} constraint) - NOT cross-checked against Admin/
     * Student, same as {@code email} already isn't. See {@code AuthService#loginParent} for the
     * actual email-OR-username-OR-phone lookup.
     */
    private String username;

    /**
     * Bumped by {@code AuthService#logoutAll()} (force-logout) - every already-issued access
     * token for this Parent embeds the tokenVersion it was minted with, and {@code
     * JwtAuthFilter} rejects a request whose token's version no longer matches this column. This
     * is also what fixes the "deleted account's old token still works" bug: {@code
     * JwtAuthFilter} loads this row by id on every request, so a Parent that no longer exists
     * (row gone) fails that lookup outright, token version aside.
     */
    private int tokenVersion = 0;

    /**
     * Whether an Admin has this account enabled (2026-09-04 - see {@code AdminParentService}).
     * Checked at login time ({@code AuthService#loginParent}/{@code #loginStudent}, the latter
     * via the student's OWN parent - deactivating a Parent blocks their Students' logins too,
     * since Parent is the tenant boundary, see this class's own javadoc). An already-logged-in
     * session is cut off within the SAME request cycle as deactivation, not just at next login -
     * {@code AdminParentService#setActive(false)} bumps {@code tokenVersion} for the Parent AND
     * every one of their Students in the same call, and {@code JwtAuthFilter} re-checks
     * tokenVersion on every request regardless of this flag.
     */
    private boolean active = true;
}
