package com.wams.service.impl;

import com.wams.dto.request.CreateUserRequest;
import com.wams.dto.request.RegisterRequest;
import com.wams.dto.request.UpdateUserRequest;
import com.wams.dto.response.UserResponse;
import com.wams.entity.Client;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.ClientRepository;
import com.wams.repository.UserRepository;
import com.wams.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           ClientRepository clientRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest r) {
        String normalizedEmail = normalizeEmail(r.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email already registered: " + normalizedEmail);
        }
        User u = User.builder()
                .fullName(r.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(r.password()))
                .role(r.role())
                .active(r.active() == null ? Boolean.TRUE : r.active())
                .build();
        User saved = userRepository.save(u);
        if (saved.getRole() == Role.PRIMARY_CLIENT || saved.getRole() == Role.PARTNER_CLIENT) {
            clientRepository.save(Client.builder().user(saved).build());
        }
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest r) {
        String normalizedEmail = normalizeEmail(r.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email already registered: " + normalizedEmail);
        }
        User u = User.builder()
                .fullName(r.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(r.password()))
                .role(Role.PRIMARY_CLIENT) // self-registration is always a primary client
                .active(true)
                .build();
        User saved = userRepository.save(u);
        // Client.user_id is non-null and unique. Persisting both rows in this
        // transaction prevents a registered login without its required profile.
        clientRepository.save(Client.builder().user(saved).build());
        return UserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listByRole(Role role) {
        return userRepository.findAllByRole(role).stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest r) {
        User u = getEntityById(id);
        if (r.fullName() != null && !r.fullName().isBlank()) {
            u.setFullName(r.fullName().trim());
        }
        if (r.email() != null && !r.email().isBlank()) {
            String newEmail = normalizeEmail(r.email());
            if (!newEmail.equalsIgnoreCase(u.getEmail())
                    && userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new DuplicateResourceException("Email already registered: " + newEmail);
            }
            u.setEmail(newEmail);
        }
        return UserResponse.from(u);
    }

    @Override
    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User u = getEntityById(id);
        u.setActive(active);
        return UserResponse.from(u);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
