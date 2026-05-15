package com.aizeronote.repository;

import com.aizeronote.model.entity.GuidanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface GuidanceSessionRepository extends JpaRepository<GuidanceSession, Long> {

    Optional<GuidanceSession> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GuidanceSession> findByIdAndUserId(Long id, Long userId);

    List<GuidanceSession> findByUserIdAndStatusInOrderByUpdatedAtDesc(Long userId, Collection<String> statuses, Pageable pageable);
}
