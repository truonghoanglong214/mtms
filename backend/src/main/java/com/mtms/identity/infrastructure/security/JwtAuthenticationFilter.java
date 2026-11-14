package com.mtms.identity.infrastructure.security;

import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserJpaRepository userRepository;
    private final ScopedRoleGrantJpaRepository grantRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   TokenBlacklistService tokenBlacklistService,
                                   UserJpaRepository userRepository,
                                   ScopedRoleGrantJpaRepository grantRepository) {
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.grantRepository = grantRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || tokenBlacklistService.isRevoked(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenService.parseAndValidateToken(token);
            String tokenType = claims.get("tokenType", String.class);
            if (!"ACCESS".equals(tokenType)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<UserJpaEntity> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    UserJpaEntity user = userOpt.get();

                    // Load active grants at request time to build authorities
                    List<ScopedRoleGrantJpaEntity> activeGrants =
                            grantRepository.findActiveGrantsByUserId(userId, LocalDate.now());

                    Set<GrantedAuthority> authorities = new HashSet<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                    for (ScopedRoleGrantJpaEntity grant : activeGrants) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + grant.getRoleKey()));
                        if (grant.getBranchId() != null) {
                            authorities.add(new SimpleGrantedAuthority("SCOPE_BRANCH_" + grant.getBranchId()));
                        }
                    }

                    UserPrincipal principal = new UserPrincipal(
                            user.getId(),
                            user.getLoginIdentifier(),
                            user.getPasswordHash(),
                            user.getDisplayName(),
                            user.getStatus(),
                            authorities
                    );

                    if (principal.isEnabled()) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        principal.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception ex) {
            // Invalid token - clear context and continue down the chain (AuthenticationEntryPoint handles rejection)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
