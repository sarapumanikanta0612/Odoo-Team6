package com.wams.service.impl;

import com.wams.dto.request.CreateUserRequest;
import com.wams.dto.request.RegisterRequest;
import com.wams.dto.request.UpdateUserRequest;
import com.wams.dto.response.UserResponse;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserServiceImpl service;

    @Test
    void create_persistsHashedPassword_andNormalizesEmail() {
        when(userRepository.existsByEmailIgnoreCase("Alice@Example.com")).thenReturn(false);
        when(passwordEncoder.encode("s3cret-pass")).thenReturn("$2a$HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        CreateUserRequest req = new CreateUserRequest(
                "Alice", "Alice@Example.com", "s3cret-pass", Role.LEAD_PLANNER, true);
        UserResponse resp = service.create(req);

        assertThat(resp.getId()).isEqualTo(42L);
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$HASHED");
        assertThat(saved.getRole()).isEqualTo(Role.LEAD_PLANNER);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void create_rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        CreateUserRequest req = new CreateUserRequest(
                "Alice", "a@x.com", "s3cret-pass", Role.EMPLOYEE, true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_alwaysAssignsPrimaryClientRole() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(new RegisterRequest("Bob", "bob@x.com", "s3cret-pass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.PRIMARY_CLIENT);
    }

    @Test
    void getById_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_rejectsEmailCollision() {
        User existing = User.builder().id(1L).email("a@x.com").role(Role.EMPLOYEE).active(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCase("b@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new UpdateUserRequest(null, "b@x.com")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void setActive_togglesFlag() {
        User existing = User.builder().id(1L).email("a@x.com").role(Role.EMPLOYEE).active(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserResponse resp = service.setActive(1L, false);
        assertThat(resp.isActive()).isFalse();
        assertThat(existing.isActive()).isFalse();
    }
}
