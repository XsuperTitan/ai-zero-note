package com.aizeronote.repository;

import com.aizeronote.model.entity.GuidanceStudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuidanceStudyPlanRepository extends JpaRepository<GuidanceStudyPlan, Long> {

    Optional<GuidanceStudyPlan> findBySession_Id(Long sessionId);

    Optional<GuidanceStudyPlan> findFirstBySessionUserIdOrderBySessionCreatedAtDesc(Long userId);
}
