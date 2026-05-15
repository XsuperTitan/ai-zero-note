package com.aizeronote.repository;

import com.aizeronote.model.entity.GuidanceCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuidanceCheckInRepository extends JpaRepository<GuidanceCheckIn, Long> {

    Optional<GuidanceCheckIn> findByIdAndUserId(Long id, Long userId);
}
