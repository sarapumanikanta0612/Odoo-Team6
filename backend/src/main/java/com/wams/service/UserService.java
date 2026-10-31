package com.wams.service;

import com.wams.dto.request.CreateUserRequest;
import com.wams.dto.request.RegisterRequest;
import com.wams.dto.request.UpdateUserRequest;
import com.wams.dto.response.UserResponse;
import com.wams.entity.User;
import com.wams.enums.Role;

import java.util.List;

public interface UserService {

    /** Admin-only. Honors supplied role (validated upstream by security rules). */
    UserResponse create(CreateUserRequest request);

    /** Public self-registration: always creates a PRIMARY_CLIENT user. */
    UserResponse register(RegisterRequest request);

    UserResponse getById(Long id);

    /** Direct access to the entity for intra-service composition. */
    User getEntityById(Long id);

    List<UserResponse> list();

    List<UserResponse> listByRole(Role role);

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse setActive(Long id, boolean active);
}
