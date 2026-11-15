package com.mtms.identity.application.dto;

import com.mtms.identity.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusCommand(
        @NotNull(message = "User status must be specified")
        UserStatus status
) {
}
