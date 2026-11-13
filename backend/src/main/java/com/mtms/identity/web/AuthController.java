package com.mtms.identity.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.identity.application.AuthenticationService;
import com.mtms.identity.application.dto.AuthResponse;
import com.mtms.identity.application.dto.LoginCommand;
import com.mtms.identity.application.dto.RefreshTokenCommand;
import com.mtms.identity.application.dto.TokenRefreshResponse;
import com.mtms.identity.application.dto.UserDto;
import com.mtms.identity.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginCommand command) {
        AuthResponse response = authenticationService.login(command);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        TokenRefreshResponse response = authenticationService.refresh(command);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        authenticationService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new com.mtms.common.error.UnauthorizedException("User is not authenticated");
        }

        UserDto profile = authenticationService.getUserProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
