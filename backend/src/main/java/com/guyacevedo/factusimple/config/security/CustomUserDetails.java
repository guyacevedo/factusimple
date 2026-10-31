package com.guyacevedo.factusimple.config.security;

import com.guyacevedo.factusimple.features.user.entity.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CustomUserDetails extends org.springframework.security.core.userdetails.User {

    private final UUID userId;

    public CustomUserDetails(String username, String password,
                             boolean enabled, boolean accountNonLocked,
                             Collection<? extends GrantedAuthority> authorities,
                             UUID userId) {
        super(username, password, enabled, true, true, accountNonLocked, authorities);
        this.userId = userId;
    }

    /**
     * Use it in your UserDetailsService and JWT filter.
     * - enabled follows is_active.
     * - locked follows User.isLocked() (temporary lock after failed logins).
     * - the authority gets the "ROLE_" prefix that hasRole('ADMIN') expects.
     */
    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getActive()),
                !user.isLocked(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.getId());
    }
}