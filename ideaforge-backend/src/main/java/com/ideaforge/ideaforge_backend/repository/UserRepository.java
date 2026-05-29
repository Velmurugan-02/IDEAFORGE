package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link User} entities.
 * Provides Spring Security integration via findByEmail.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by Spring Security to load a user during authentication. */
    Optional<User> findByEmail(String email);

    /** Check for duplicate email during registration. */
    boolean existsByEmail(String email);
}
