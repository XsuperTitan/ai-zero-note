package com.aizeronote.repository;

import com.aizeronote.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserAccount(String userAccount);

    boolean existsByUserAccount(String userAccount);
}
