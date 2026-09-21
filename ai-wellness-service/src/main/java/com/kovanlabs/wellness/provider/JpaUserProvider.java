package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserProvider implements UserProvider {

    private final UserRepository userRepository;

    public JpaUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public java.util.List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
