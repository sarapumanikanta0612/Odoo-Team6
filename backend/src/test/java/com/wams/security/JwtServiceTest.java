package com.wams.security;

import com.wams.entity.User;
import com.wams.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String STRONG_TEST_SECRET =
            "correct-horse-battery-staple-for-wams-tests-2026-abcdef";

    @Test
    void constructor_rejectsMissingShortAndRemovedFallbackSecrets() {
        assertThatThrownBy(() -> new JwtService(null, 60_000, "wams"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> new JwtService("too-short", 60_000, "wams"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new JwtService(
                "dev-only-insecure-change-me-at-least-32-bytes-long-xxxxxxxxxxxxxxxxxxxx",
                60_000,
                "wams"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure");
    }

    @Test
    void constructor_rejectsInvalidLifetimeAndIssuer() {
        assertThatThrownBy(() -> new JwtService(STRONG_TEST_SECRET, 0, "wams"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> new JwtService(STRONG_TEST_SECRET, 60_000, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void generatedToken_containsStableIdentityAndParsesWithConfiguredIssuer() {
        JwtService service = new JwtService(STRONG_TEST_SECRET, 90_000, "wams-test");
        User user = User.builder()
                .id(42L)
                .email("admin@wams.test")
                .role(Role.ADMIN)
                .active(true)
                .build();

        Claims claims = service.parse(service.generateToken(user));

        assertThat(claims.getSubject()).isEqualTo("admin@wams.test");
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(service.getExpirationSeconds()).isEqualTo(90L);
    }

    @Test
    void tokenSignedByAnotherSecret_isRejected() {
        JwtService issuer = new JwtService(STRONG_TEST_SECRET, 60_000, "wams-test");
        JwtService verifier = new JwtService(
                "another-secure-wams-test-secret-with-at-least-thirty-two-bytes",
                60_000,
                "wams-test");
        User user = User.builder().id(1L).email("a@wams.test").role(Role.ADMIN).active(true).build();

        assertThatThrownBy(() -> verifier.parse(issuer.generateToken(user)))
                .isInstanceOf(RuntimeException.class);
    }
}
