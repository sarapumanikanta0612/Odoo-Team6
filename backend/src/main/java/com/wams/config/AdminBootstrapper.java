package com.wams.config;

import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Creates the first ADMIN and its required Employee profile when bootstrap
 * credentials are configured. Re-running is safe: existing users and profiles
 * are never overwritten or duplicated.
 */
@Component
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);
    private static final String DEFAULT_ADMIN_NAME = "Administrator";
    private static final String DEFAULT_EMPLOYEE_CODE = "ADMIN-001";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredEmail;
    private final String configuredPassword;
    private final String configuredFullName;
    private final String configuredEmployeeCode;

    public AdminBootstrapper(UserRepository userRepository,
                             EmployeeRepository employeeRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.bootstrap.admin.email:}") String configuredEmail,
                             @Value("${app.bootstrap.admin.password:}") String configuredPassword,
                             @Value("${app.bootstrap.admin.full-name:Administrator}") String configuredFullName,
                             @Value("${app.bootstrap.admin.employee-code:ADMIN-001}") String configuredEmployeeCode) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredEmail = configuredEmail;
        this.configuredPassword = configuredPassword;
        this.configuredFullName = configuredFullName;
        this.configuredEmployeeCode = configuredEmployeeCode;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!hasText(configuredEmail) || !hasText(configuredPassword)) {
            log.warn("Admin bootstrap skipped: set BOOTSTRAP_ADMIN_EMAIL and " +
                    "BOOTSTRAP_ADMIN_PASSWORD to seed the first administrator.");
            return;
        }

        String normalizedEmail = configuredEmail.trim().toLowerCase(Locale.ROOT);
        Optional<User> configuredUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (configuredUser.isPresent()) {
            User user = configuredUser.get();
            if (user.getRole() != Role.ADMIN) {
                log.warn("Admin bootstrap skipped: configured email belongs to a non-admin account. email='{}'",
                        normalizedEmail);
                return;
            }
            ensureEmployeeProfile(user);
            log.info("Admin bootstrap: configured ADMIN already exists. email='{}'", normalizedEmail);
            return;
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin bootstrap skipped: at least one ADMIN account already exists.");
            return;
        }

        String fullName = hasText(configuredFullName) ? configuredFullName.trim() : DEFAULT_ADMIN_NAME;
        User admin = userRepository.save(User.builder()
                .fullName(fullName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(configuredPassword))
                .role(Role.ADMIN)
                .active(true)
                .build());
        ensureEmployeeProfile(admin);
        log.info("Admin bootstrap: created initial ADMIN account and Employee profile. email='{}'", normalizedEmail);
    }

    private void ensureEmployeeProfile(User admin) {
        if (employeeRepository.existsByUserId(admin.getId())) {
            return;
        }
        String employeeCode = hasText(configuredEmployeeCode)
                ? configuredEmployeeCode.trim()
                : DEFAULT_EMPLOYEE_CODE;
        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new IllegalStateException(
                    "Cannot create bootstrap admin Employee profile: employee code already exists: " + employeeCode);
        }
        employeeRepository.save(Employee.builder()
                .user(admin)
                .employeeCode(employeeCode)
                .designation("Administrator")
                .department("Management")
                .joiningDate(LocalDate.now())
                .active(true)
                .build());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
