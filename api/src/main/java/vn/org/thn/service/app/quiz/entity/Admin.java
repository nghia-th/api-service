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
 * deactivate/delete, see {@code AdminParentApi}), and nothing else in v1 (no Admin-vs-Admin
 * management, no permission levels - a single flat role, same as {@link Parent}/{@link Student}
 * each being their own flat role).
 * <p>
 * <b>No self-registration</b> - unlike Parent, there is no {@code POST /api/auth/admin/register}.
 * The first (and, in v1, only) Admin row is created at startup by {@code
 * config/AdminBootstrapRunner} from {@code quiz.admin.bootstrap-email}/{@code
 * bootstrap-password} in {@code application.yaml} if the {@code admin} table is empty - see that
 * class's javadoc for why (bootstrapping problem: nothing/no-one exists yet to call a "create
 * admin" endpoint with).
 * <p>
 * {@code tokenVersion} - same purpose/mechanism as {@link Parent#getTokenVersion()}/{@link
 * Student#getTokenVersion()} (force-logout, see {@code AuthService}'s javadoc) - included here
 * too so an Admin session can also be force-logged-out consistently, even though nothing calls
 * that for Admin yet in v1.
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
}
