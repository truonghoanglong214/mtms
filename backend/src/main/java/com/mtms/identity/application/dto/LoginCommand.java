package com.mtms.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCommand(
        @NotBlank(message = "Login identifier must not be blank")
        @Size(max = 200, message = "Login identifier must not exceed 200 characters")
        String loginIdentifier,

        @NotBlank(message = "Password must not be blank")
        String password
) {
}
