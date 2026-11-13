package com.mtms.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.error.GlobalExceptionHandler;
import com.mtms.identity.application.AuthenticationService;
import com.mtms.identity.application.dto.AuthResponse;
import com.mtms.identity.application.dto.LoginCommand;
import com.mtms.identity.application.dto.RefreshTokenCommand;
import com.mtms.identity.application.dto.TokenRefreshResponse;
import com.mtms.identity.application.dto.UserDto;
import com.mtms.identity.application.exception.AccountSuspendedException;
import com.mtms.identity.application.exception.AuthenticationFailedException;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.security.SecurityConfig;
import com.mtms.identity.infrastructure.security.UserPrincipal;
import com.mtms.identity.web.AuthController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with tokens on valid credentials")
    void testLoginSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "admin", "System Administrator", UserStatus.ACTIVE, Instant.now());
        AuthResponse authResponse = AuthResponse.of("mock-access-token", "mock-refresh-token", 3600, userDto);

        when(authenticationService.login(any(LoginCommand.class))).thenReturn(authResponse);

        LoginCommand cmd = new LoginCommand("admin", "P@ssword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.data.user.loginIdentifier").value("admin"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 400 when loginIdentifier is blank")
    void testLoginValidationBlankIdentifier() throws Exception {
        LoginCommand cmd = new LoginCommand("", "P@ssword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:validation-error"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 400 when password is blank")
    void testLoginValidationBlankPassword() throws Exception {
        LoginCommand cmd = new LoginCommand("admin", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:validation-error"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 when credentials are bad")
    void testLoginBadCredentials() throws Exception {
        when(authenticationService.login(any(LoginCommand.class)))
                .thenThrow(new AuthenticationFailedException("Invalid login identifier or password"));

        LoginCommand cmd = new LoginCommand("admin", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"))
                .andExpect(jsonPath("$.detail").value("Invalid login identifier or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 403 when account is suspended")
    void testLoginAccountSuspended() throws Exception {
        when(authenticationService.login(any(LoginCommand.class)))
                .thenThrow(new AccountSuspendedException("User account is SUSPENDED"));

        LoginCommand cmd = new LoginCommand("locked_user", "P@ssword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"))
                .andExpect(jsonPath("$.detail").value("User account is SUSPENDED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh should return 200 with new access token")
    void testRefreshTokenSuccess() throws Exception {
        TokenRefreshResponse refreshResponse = TokenRefreshResponse.of("new-mock-access-token", 3600);
        when(authenticationService.refresh(any(RefreshTokenCommand.class))).thenReturn(refreshResponse);

        RefreshTokenCommand cmd = new RefreshTokenCommand("valid-mock-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-mock-access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(3600));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout should return 200 and invoke service logout")
    void testLogoutSuccess() throws Exception {
        doNothing().when(authenticationService).logout("Bearer mock-token-to-revoke");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer mock-token-to-revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 200 and user profile when authenticated")
    void testGetCurrentUserSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId,
                "current_admin",
                "hashed",
                "Current Admin",
                UserStatus.ACTIVE
        );
        UserDto userDto = new UserDto(userId, "current_admin", "Current Admin", UserStatus.ACTIVE, Instant.now());

        when(authenticationService.getUserProfile(userId)).thenReturn(userDto);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginIdentifier").value("current_admin"))
                .andExpect(jsonPath("$.data.displayName").value("Current Admin"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 401 when unauthenticated")
    void testGetCurrentUserUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"));
    }
}
