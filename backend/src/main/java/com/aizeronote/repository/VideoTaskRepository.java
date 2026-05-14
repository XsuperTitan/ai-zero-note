package com.aizeronote.repository;

import com.aizeronote.model.entity.VideoTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoTaskRepository extends JpaRepository<VideoTask, Long> {

    Page<VideoTask> findAllByUserId(Long userId, Pageable pageable);

    Optional<VideoTask> findByTaskIdAndUserId(String taskId, Long userId);

    boolean existsByUserIdAndTaskId(Long userId, String taskId);
}
