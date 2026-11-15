package com.mtms.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserCommand(
        @NotBlank(message = "Login identifier must not be blank")
        @Size(min = 3, max = 200, message = "Login identifier must be between 3 and 200 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Login identifier can only contain alphanumeric characters, underscores, hyphens, and periods")
        String loginIdentifier,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 120, message = "Password must be at least 8 characters long")
        String password,

        @NotBlank(message = "Display name must not be blank")
        @Size(min = 2, max = 160, message = "Display name must be between 2 and 160 characters")
        String displayName
) {
}
