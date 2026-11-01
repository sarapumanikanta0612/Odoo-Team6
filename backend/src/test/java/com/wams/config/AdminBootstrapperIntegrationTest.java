package com.wams.config;

import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.repository.ClientRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification of the admin bootstrap plus the invariant that public
 * registration never produces an ADMIN account.
 */
@SpringBootTest(properties = {
        "app.bootstrap.admin.email=bootstrap-admin@wams.test",
        "app.bootstrap.admin.password=s3cret-admin-pass",
        "app.bootstrap.admin.full-name=Bootstrap Admin",
        "app.bootstrap.admin.employee-code=BOOTSTRAP-ADMIN"
})
@AutoConfigureMockMvc
class AdminBootstrapperIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AdminBootstrapper bootstrapper;
    @Autowired MockMvc mvc;

    @Test
    void bootstrap_createsFirstAdmin_withBcryptHash() {
        Optional<User> admin = userRepository.findByEmailIgnoreCase("bootstrap-admin@wams.test");
        assertThat(admin).as("Admin should have been created on context start").isPresent();
        assertThat(admin.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.get().isActive()).isTrue();
        assertThat(admin.get().getPasswordHash())
                .as("Password must be stored as a BCrypt hash, never plain text")
                .startsWith("$2");
        assertThat(passwordEncoder.matches("s3cret-admin-pass", admin.get().getPasswordHash()))
                .as("BCrypt match should succeed")
                .isTrue();
        assertThat(employeeRepository.findByUserId(admin.get().getId()))
                .as("Bootstrap ADMIN needs an Employee profile so it can lead projects")
                .get()
                .satisfies(employee -> {
                    assertThat(employee.getEmployeeCode()).isEqualTo("BOOTSTRAP-ADMIN");
                    assertThat(employee.isActive()).isTrue();
                });
    }

    @Test
    void bootstrap_isIdempotent_whenRunnerInvokedAgain_doesNotCreateDuplicateAdmin() {
        // Note: this test does NOT restart the Spring context. It invokes the
        // existing AdminBootstrapper bean a second time within the same
        // ApplicationContext (same JVM, same in-memory DB). The guarantee under
        // test is that the component's own idempotency check (existsByRole)
        // prevents a duplicate ADMIN row, which is the same behavior that
        // would protect a real application restart against the same DB.
        long adminsBefore = userRepository.findAllByRole(Role.ADMIN).size();
        long employeesBefore = employeeRepository.count();
        bootstrapper.run(null);
        long adminsAfter = userRepository.findAllByRole(Role.ADMIN).size();
        long employeesAfter = employeeRepository.count();
        assertThat(adminsAfter).isEqualTo(adminsBefore);
        assertThat(employeesAfter).isEqualTo(employeesBefore);
    }

    @Test
    void publicRegistration_cannotCreateAdmin_evenWithExplicitRoleInBody() throws Exception {
        // RegisterRequest has no role field at all, so even if a crafted body
        // includes "role":"ADMIN", Jackson ignores unknown fields and the server
        // still assigns PRIMARY_CLIENT. We assert both: 201 success AND role.
        String body = """
                {
                  "fullName": "Hacker Alice",
                  "email": "hacker.alice@wams.test",
                  "password": "s3cret-password",
                  "role": "ADMIN"
                }
                """;
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PRIMARY_CLIENT"));

        User created = userRepository.findByEmailIgnoreCase("hacker.alice@wams.test").orElseThrow();
        assertThat(created.getRole())
                .as("Public registration must never produce an ADMIN")
                .isEqualTo(Role.PRIMARY_CLIENT);
        assertThat(clientRepository.findByUserId(created.getId()))
                .as("Registration must atomically create the required Client profile")
                .isPresent();
    }
}
