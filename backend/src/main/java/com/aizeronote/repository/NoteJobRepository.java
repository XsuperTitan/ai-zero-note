package com.aizeronote.repository;

import com.aizeronote.model.entity.NoteJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteJobRepository extends JpaRepository<NoteJob, Long> {

    Page<NoteJob> findAllByUserId(Long userId, Pageable pageable);

    Optional<NoteJob> findByNoteIdAndUserId(String noteId, Long userId);

    boolean existsByUserIdAndMarkdownFileName(Long userId, String markdownFileName);
}
