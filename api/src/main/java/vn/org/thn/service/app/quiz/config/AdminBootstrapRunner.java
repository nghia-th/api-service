package vn.org.thn.service.app.quiz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import vn.org.thn.service.app.quiz.entity.Admin;
import vn.org.thn.service.app.quiz.repository.AdminRepository;

import java.time.LocalDateTime;

/**
 * Creates the FIRST Admin row at startup, if the {@code admin} table is still empty - there is no
 * {@code POST /api/auth/admin/register} (see {@code entity/Admin.java}'s javadoc), so without
 * this, nobody could ever log in as Admin at all: every other account type in quiz-service is
 * created either by self-registration (Parent) or by an already-logged-in account of a higher
 * tier (Student, by its Parent) - Admin has no higher tier to be created by, so it has to bootstrap
 * itself from static config instead.
 * <p>
 * <b>Fixed {@code root}/{@code root} login (2026-09-04, replacing the earlier {@code
 * quiz.admin.bootstrap-email}/{@code bootstrap-password} application.yaml config):</b> per the
 * user's explicit request, the very first Admin account is now always email/username {@code
 * "root"}, initial password {@code "root"} - no config file to edit before first boot. This
 * account is also flagged {@link Admin#isRoot()}{@code =true} (see that field's javadoc - marks
 * it undeletable, forward-looking, no enforcement point exists yet). The password is NOT meant to
 * stay {@code "root"} - change it immediately after first login via the new self-service
 * change-password endpoint ({@code POST /api/admin/change-password}, see {@code AuthApi}); unlike
 * the old application.yaml-config approach, this is now the ONLY way to change it (no more "edit
 * the DB by hand" gap).
 * <p>
 * Only ever creates the row if MISSING - checked by {@code email = ROOT_EMAIL} specifically
 * (2026-09-06, fixed - see below), NOT "table is empty", so root's password (once changed) is
 * never silently reset back to {@code "root"} by a restart.
 * <p>
 * <b>2026-09-06 bug fix:</b> the ORIGINAL check here was {@code adminRepository.exists()} (ANY
 * row present, table-wide) - anh reported after testing that the root account was never actually
 * created. Root cause: this service pre-dates the {@code root}/{@code root} feature - a database
 * that already had 1+ Admin row from the OLD {@code quiz.admin.bootstrap-email}/{@code
 * bootstrap-password} application.yaml-config bootstrap (or any other pre-existing Admin) makes
 * {@code exists()} true forever, so this runner silently no-ops on EVERY startup and root never
 * gets created - the exact symptom anh hit. Fixed by checking for {@code email = ROOT_EMAIL}
 * specifically instead of "any row" - this guarantees the root account gets created exactly once
 * whenever it is missing, regardless of how many OTHER Admin rows already exist, while still
 * never touching/recreating it once it exists (so a changed root password is still never reset).
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    /** The fixed root login - see this class's javadoc, "Fixed root/root login". Not configurable; change the password after first login instead. */
    private static final String ROOT_EMAIL = "root";
    private static final String ROOT_INITIAL_PASSWORD = "root";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.query().eq(Admin::getEmail, ROOT_EMAIL).exists()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Admin admin = new Admin();
        admin.setEmail(ROOT_EMAIL);
        admin.setPassword(passwordEncoder.encode(ROOT_INITIAL_PASSWORD));
        admin.setFullName("Administrator");
        admin.setRoot(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setCreatedBy("system:bootstrap");
        admin.setUpdatedBy("system:bootstrap");
        adminRepository.save(admin);

        log.warn("Bootstrapped the first Admin account (email='root', password='root', root=true) - " +
                "CHANGE THE PASSWORD IMMEDIATELY after first login via POST /api/admin/change-password, " +
                "especially before any non-local deployment.");
    }
}
