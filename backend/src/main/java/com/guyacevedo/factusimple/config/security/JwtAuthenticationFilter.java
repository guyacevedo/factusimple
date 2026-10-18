package com.guyacevedo.factusimple.config.security;

import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads "Authorization: Bearer <access token>" and, if the token is valid,
 * puts a CustomUserDetails in the SecurityContext (what @AuthenticationPrincipal
 * and the JPA auditor read).
 *
 * It never rejects a request itself: with no valid token the request continues
 * as anonymous, and SecurityConfig answers 401 on protected routes. Public
 * routes keep working even if the browser sends an expired token.
 *
 * Not a @Component on purpose: SecurityConfig creates it, so Spring Boot does
 * not register it a second time as a plain servlet filter.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Only access tokens: a refresh token in this header is rejected.
            // The user is loaded on each request (one query by primary key), so
            // disabling an account takes effect immediately, not when the token expires.
            jwtTokenProvider.parseAccessToken(token)
                    .flatMap(claims -> userRepository.findById(claims.userId()))
                    .filter(user -> Boolean.TRUE.equals(user.getActive()))
                    .map(CustomUserDetails::from)
                    .ifPresentOrElse(
                            principal -> authenticate(principal, request),
                            () -> log.debug("Access token rechazado en {}", request.getRequestURI()));
        }

        filterChain.doFilter(request, response);
    }

    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static void authenticate(CustomUserDetails principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}