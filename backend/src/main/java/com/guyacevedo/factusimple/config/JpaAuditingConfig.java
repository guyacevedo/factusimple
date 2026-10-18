package com.guyacevedo.factusimple.config;

import com.guyacevedo.factusimple.features.user.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                .flatMap(JpaAuditingConfig::resolveUserId);
    }

    private static Optional<UUID> resolveUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof User user) {
            return Optional.ofNullable(user.getId());
        }
        try {
            // Works when the JWT subject is the user id.
            return Optional.of(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }
}