package com.wams.config;

import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapperTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AdminBootstrapper bootstrapper(String email, String password) {
        return new AdminBootstrapper(userRepository, passwordEncoder, email, password, "Administrator");
    }

    @Test
    void skips_whenEmailBlank_doesNotTouchRepository() {
        bootstrapper("", "s3cret-password").run(null);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skips_whenPasswordBlank_doesNotTouchRepository() {
        bootstrapper("admin@wams.test", "   ").run(null);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skips_whenBothBlank_doesNotTouchRepository() {
        bootstrapper(null, null).run(null);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skips_whenAdminAlreadyExists_doesNotSave() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrapper("admin@wams.test", "s3cret-password").run(null);

        verify(userRepository).existsByRole(Role.ADMIN);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skips_whenEmailAlreadyExists_butNoAdmin_doesNotOverwrite() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@wams.test")).thenReturn(true);

        bootstrapper("admin@wams.test", "s3cret-password").run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createsAdmin_hashesViaEncoder_normalizesEmail() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("s3cret-password")).thenReturn("$2a$HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        bootstrapper("Admin@WAMS.Test", "s3cret-password").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@wams.test");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$HASHED");
        verify(passwordEncoder).encode("s3cret-password");
    }

    @Test
    void createsAdmin_lowercasesEmailWithLocaleRoot_notJvmDefaultLocale() {
        // Under the Turkish locale, 'I' lowercases to 'ı' (dotless) instead of
        // 'i' (dotted) when the default locale is used. We force the JVM default
        // to Turkish for this test and verify the Locale.ROOT path returns 'i',
        // so email lookups remain reliable regardless of where the server runs.
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            bootstrapper("ADMIN@WAMS.TEST", "s3cret-password").run(null);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("admin@wams.test");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void createsAdmin_fallsBackToAdministrator_whenNameBlank() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        new AdminBootstrapper(userRepository, passwordEncoder,
                "admin@wams.test", "s3cret-password", "   ").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("Administrator");
    }

    @Test
    void createsAdmin_fallsBackToAdministrator_whenNameNull() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        new AdminBootstrapper(userRepository, passwordEncoder,
                "admin@wams.test", "s3cret-password", null).run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("Administrator");
    }
}
