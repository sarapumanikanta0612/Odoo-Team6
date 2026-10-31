package com.wams.controller;

import com.wams.dto.request.CreateUserRequest;
import com.wams.dto.request.UpdateUserRequest;
import com.wams.dto.request.UpdateUserStatusRequest;
import com.wams.dto.response.UserResponse;
import com.wams.enums.Role;
import com.wams.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list(@RequestParam(value = "role", required = false) Role role) {
        return ResponseEntity.ok(role == null ? userService.list() : userService.listByRole(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> setStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserStatusRequest req) {
        return ResponseEntity.ok(userService.setActive(id, req.active()));
    }
}
