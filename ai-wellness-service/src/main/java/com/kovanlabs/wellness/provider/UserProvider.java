package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.UserEntity;

import java.util.Optional;

/**
 * Abstraction layer for user data access.
 */
public interface UserProvider {

    UserEntity save(UserEntity user);

    Optional<UserEntity> findById(Long id);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(Long id);
}
