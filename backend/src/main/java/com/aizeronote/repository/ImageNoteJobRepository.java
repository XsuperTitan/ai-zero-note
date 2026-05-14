package com.aizeronote.repository;

import com.aizeronote.model.entity.ImageNoteJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageNoteJobRepository extends JpaRepository<ImageNoteJob, Long> {

    Optional<ImageNoteJob> findByJobId(String jobId);

    Optional<ImageNoteJob> findByJobIdAndUserId(String jobId, Long userId);

    Optional<ImageNoteJob> findByImageFileNameAndUserId(String imageFileName, Long userId);
}
