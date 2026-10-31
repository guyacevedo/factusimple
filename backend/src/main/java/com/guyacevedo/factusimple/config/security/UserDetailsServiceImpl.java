package com.guyacevedo.factusimple.config.security;

import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring turns UsernameNotFoundException into BadCredentialsException, so the
    // client always sees "Email o contraseña incorrectos" and cannot tell which failed.
    // The message carries no email to keep it out of the logs.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email)
                .map(CustomUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}