package com.bookworm.common.security;

import com.bookworm.user.entity.User;
import com.bookworm.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        String roleName = user.getRole() != null ? user.getRole().name() : UserRole.ROLE_CUSTOMER.name();
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));

        return UserPrincipal.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .role(user.getRole())
                .authorities(authorities)
                .build();
    }

    public static UserPrincipal createGuest(String sessionToken) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(UserRole.ROLE_GUEST.name())
        );

        return UserPrincipal.builder()
                .userId(null)
                .email("guest_" + sessionToken + "@bookworm.local")
                .password("")
                .role(UserRole.ROLE_GUEST)
                .authorities(authorities)
                .build();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
