package com.ideaforge.ideaforge_backend.security;

import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security hook that loads a {@link User} by email from the database.
 *
 * Wired into the AuthenticationProvider in {@link SecurityConfig}.
 * Because {@link User} implements {@link UserDetails}, no adapter wrapper is needed.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security calls this method with the value the user supplied as
     * their "username" — which in IdeaForge is their email address.
     *
     * @param email the email supplied during authentication
     * @return the matching {@link User} entity (which also implements UserDetails)
     * @throws UsernameNotFoundException if no user exists with this email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));
    }
}
