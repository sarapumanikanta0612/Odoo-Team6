package com.wams.config;

import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Seeds the first ADMIN account on application startup when
 * {@code BOOTSTRAP_ADMIN_EMAIL} and {@code BOOTSTRAP_ADMIN_PASSWORD} are both
 * set in the environment.
 *
 * Guarantees:
 * <ul>
 *   <li>Idempotent: if any ADMIN already exists, the bootstrap is a no-op.</li>
 *   <li>Non-destructive: never overwrites or demotes an existing account; if a
 *       user with the configured email exists but is not an ADMIN, the
 *       bootstrap is skipped with a warning.</li>
 *   <li>Transactional: runs inside a single transaction so a partial write
 *       cannot land in the database.</li>
 *   <li>Credential-safe: passwords are hashed via the existing BCrypt
 *       {@link PasswordEncoder}; nothing related to the password is ever
 *       written to logs.</li>
 *   <li>Separation of concerns: the public registration flow is unchanged and
 *       still cannot produce ADMIN accounts; this is the ONLY path that can
 *       create one without an existing administrator.</li>
 * </ul>
 */
@Component
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredEmail;
    private final String configuredPassword;
    private final String configuredFullName;

    public AdminBootstrapper(UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.bootstrap.admin.email:}") String configuredEmail,
                             @Value("${app.bootstrap.admin.password:}") String configuredPassword,
                             @Value("${app.bootstrap.admin.full-name:Administrator}") String configuredFullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredEmail = configuredEmail;
        this.configuredPassword = configuredPassword;
        this.configuredFullName = configuredFullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!hasText(configuredEmail) || !hasText(configuredPassword)) {
            log.warn("Admin bootstrap skipped: set BOOTSTRAP_ADMIN_EMAIL and " +
                    "BOOTSTRAP_ADMIN_PASSWORD to seed the first administrator.");
            return;
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin bootstrap skipped: at least one ADMIN account already exists.");
            return;
        }
        // Normalize using Locale.ROOT so emails are lowercased in a locale-neutral
        // way. The JVM default locale could otherwise mangle characters (e.g.
        // Turkish-locale 'I' lowercases to 'ı' rather than 'i'), producing an
        // email that would never match the one used at login/lookup time.
        String normalizedEmail = configuredEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            // A non-admin user already uses this email. We refuse to overwrite
            // or promote; the operator must resolve the conflict manually.
            log.warn("Admin bootstrap skipped: an account with the configured email already " +
                    "exists but is not an ADMIN; refusing to overwrite. email='{}'", normalizedEmail);
            return;
        }
        String fullName = hasText(configuredFullName) ? configuredFullName.trim() : "Administrator";
        User admin = User.builder()
                .fullName(fullName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(configuredPassword))
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
        log.info("Admin bootstrap: created initial ADMIN account. email='{}'", normalizedEmail);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
