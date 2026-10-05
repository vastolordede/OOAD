package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String email,
            String fullName,
            Pageable pageable
    );
}
