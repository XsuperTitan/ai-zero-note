package com.aizeronote.repository;

import com.aizeronote.model.entity.GuidanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuidanceSessionRepository extends JpaRepository<GuidanceSession, Long> {

    Optional<GuidanceSession> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GuidanceSession> findByIdAndUserId(Long id, Long userId);
}
