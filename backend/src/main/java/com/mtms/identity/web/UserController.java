package com.mtms.identity.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.identity.application.UserService;
import com.mtms.identity.application.dto.CreateUserCommand;
import com.mtms.identity.application.dto.UpdateUserStatusCommand;
import com.mtms.identity.application.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * P0-ORG-06 / BR-ORG-01
 * User Management Controller for administrators.
 * Protected by System Admin role to ensure security isolation.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.isSystemAdmin() or @branchAuth.getCurrentUserId() == #id")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserCommand command) {
        UserDto created = userService.createUser(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "User created successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusCommand command) {
        UserDto updated = userService.updateUserStatus(id, command);
        return ResponseEntity.ok(ApiResponse.ok(updated, "User status updated successfully"));
    }
}
