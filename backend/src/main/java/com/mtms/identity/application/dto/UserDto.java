package com.mtms.identity.application.dto;

import com.mtms.identity.domain.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        String loginIdentifier,
        String displayName,
        UserStatus status,
        Instant createdAt,
        List<ScopedRoleGrantDto> grants
) {
    public UserDto(UUID id, String loginIdentifier, String displayName, UserStatus status, Instant createdAt) {
        this(id, loginIdentifier, displayName, status, createdAt, List.of());
    }
}
