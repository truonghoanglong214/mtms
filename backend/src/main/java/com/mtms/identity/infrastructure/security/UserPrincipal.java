package com.mtms.identity.infrastructure.security;

import com.mtms.identity.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String loginIdentifier;
    private final String passwordHash;
    private final String displayName;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id,
                         String loginIdentifier,
                         String passwordHash,
                         String displayName,
                         UserStatus status) {
        this(id, loginIdentifier, passwordHash, displayName, status, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public UserPrincipal(UUID id,
                         String loginIdentifier,
                         String passwordHash,
                         String displayName,
                         UserStatus status,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.loginIdentifier = loginIdentifier;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.authorities = (authorities != null && !authorities.isEmpty())
                ? Collections.unmodifiableCollection(authorities)
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public boolean hasAuthority(String authorityName) {
        if (authorityName == null || authorities == null) return false;
        return authorities.stream().anyMatch(a -> authorityName.equals(a.getAuthority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginIdentifier;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
