package com.mtms.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(
        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {
}
