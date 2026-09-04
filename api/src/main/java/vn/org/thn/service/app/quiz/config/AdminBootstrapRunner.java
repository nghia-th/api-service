package vn.org.thn.service.app.quiz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * Credentials come from {@code quiz.admin.bootstrap-email}/{@code bootstrap-password} in {@code
 * application.yaml} - DEV-ONLY placeholders, same warning as {@code quiz.jwt.secret}: override
 * both via an env-specific config before any non-local deployment, and change the password
 * immediately after first login (there is no "change own password" endpoint yet in v1 - left as a
 * known gap, see project doc).
 * <p>
 * Only ever creates the FIRST row - if an Admin already exists (table non-empty), this is a no-op
 * on every subsequent startup, even if the configured bootstrap email/password later change (that
 * would silently do nothing rather than either creating a confusing second Admin or overwriting
 * the first one's real, possibly since-changed, password).
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private AdminRepository adminRepository;

    @Value("${quiz.admin.bootstrap-email:admin@example.com}")
    private String bootstrapEmail;

    @Value("${quiz.admin.bootstrap-password:ChangeMe123!}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.exists()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Admin admin = new Admin();
        admin.setEmail(bootstrapEmail);
        admin.setPassword(passwordEncoder.encode(bootstrapPassword));
        admin.setFullName("Administrator");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setCreatedBy("system:bootstrap");
        admin.setUpdatedBy("system:bootstrap");
        adminRepository.save(admin);

        log.warn("Bootstrapped the first Admin account (email={}) from quiz.admin.bootstrap-* config - " +
                "change its password via the database directly if this is not a fresh dev environment, " +
                "there is no change-password endpoint yet.", bootstrapEmail);
    }
}
