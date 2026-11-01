package com.wams.config;

import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapperTest {

    @Mock UserRepository userRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AdminBootstrapper bootstrapper(String email, String password) {
        return new AdminBootstrapper(
                userRepository, employeeRepository, passwordEncoder,
                email, password, "Administrator", "ADMIN-001");
    }

    @Test
    void skips_whenEmailBlank_doesNotTouchRepositories() {
        bootstrapper("", "s3cret-password").run(null);
        verifyNoInteractions(userRepository, employeeRepository, passwordEncoder);
    }

    @Test
    void skips_whenPasswordBlank_doesNotTouchRepositories() {
        bootstrapper("admin@wams.test", "   ").run(null);
        verifyNoInteractions(userRepository, employeeRepository, passwordEncoder);
    }

    @Test
    void skips_whenAnotherAdminAlreadyExists_doesNotSave() {
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.empty());
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrapper("admin@wams.test", "s3cret-password").run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(employeeRepository, passwordEncoder);
    }

    @Test
    void skips_whenConfiguredEmailBelongsToNonAdmin() {
        User client = User.builder().id(9L).role(Role.PRIMARY_CLIENT).active(true).build();
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.of(client));

        bootstrapper("admin@wams.test", "s3cret-password").run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(employeeRepository, passwordEncoder);
    }

    @Test
    void createsAdminAndLinkedEmployee_hashesPassword_andNormalizesEmail() {
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.empty());
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("s3cret-password")).thenReturn("$2a$HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(employeeRepository.existsByUserId(42L)).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrapper("Admin@WAMS.Test", "s3cret-password").run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@wams.test");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$HASHED");

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee employee = employeeCaptor.getValue();
        assertThat(employee.getUser()).isSameAs(savedUser);
        assertThat(employee.getEmployeeCode()).isEqualTo("ADMIN-001");
        assertThat(employee.getDesignation()).isEqualTo("Administrator");
        assertThat(employee.getDepartment()).isEqualTo("Management");
        assertThat(employee.isActive()).isTrue();
    }

    @Test
    void existingConfiguredAdmin_missingProfile_isBackfilledWithoutChangingUser() {
        User admin = User.builder().id(10L).email("admin@wams.test").role(Role.ADMIN).active(true).build();
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.of(admin));
        when(employeeRepository.existsByUserId(10L)).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(false);

        bootstrapper("admin@wams.test", "s3cret-password").run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
        verify(employeeRepository).save(argThat(employee -> employee.getUser() == admin));
    }

    @Test
    void existingConfiguredAdmin_withProfile_isIdempotent() {
        User admin = User.builder().id(10L).email("admin@wams.test").role(Role.ADMIN).active(true).build();
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.of(admin));
        when(employeeRepository.existsByUserId(10L)).thenReturn(true);

        bootstrapper("admin@wams.test", "s3cret-password").run(null);

        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never()).existsByEmployeeCode(anyString());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void profileBackfill_rejectsEmployeeCodeCollision() {
        User admin = User.builder().id(10L).email("admin@wams.test").role(Role.ADMIN).active(true).build();
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.of(admin));
        when(employeeRepository.existsByUserId(10L)).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(true);

        assertThatThrownBy(() -> bootstrapper("admin@wams.test", "s3cret-password").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("employee code already exists");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createsAdmin_lowercasesEmailWithLocaleRoot_notJvmDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.empty());
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(employeeRepository.existsByUserId(1L)).thenReturn(false);
            when(employeeRepository.existsByEmployeeCode(anyString())).thenReturn(false);

            bootstrapper("ADMIN@WAMS.TEST", "s3cret-password").run(null);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("admin@wams.test");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void createsAdmin_fallsBackToDefaults_whenNameAndCodeBlank() {
        when(userRepository.findByEmailIgnoreCase("admin@wams.test")).thenReturn(Optional.empty());
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(employeeRepository.existsByUserId(1L)).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(false);

        new AdminBootstrapper(userRepository, employeeRepository, passwordEncoder,
                "admin@wams.test", "s3cret-password", "   ", " ").run(null);

        verify(userRepository).save(argThat(user -> "Administrator".equals(user.getFullName())));
        verify(employeeRepository).save(argThat(employee -> "ADMIN-001".equals(employee.getEmployeeCode())));
    }
}
