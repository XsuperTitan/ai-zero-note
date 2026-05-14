package com.aizeronote.service;

import com.aizeronote.model.NoteResult;
import com.aizeronote.model.entity.NoteJob;
import com.aizeronote.repository.NoteJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteJobService {

    private static final Logger log = LoggerFactory.getLogger(NoteJobService.class);
    private static final String DOWNLOAD_PREFIX = "/api/notes/download/";
    private static final int LABEL_MAX = 512;

    private final NoteJobRepository noteJobRepository;

    public NoteJobService(NoteJobRepository noteJobRepository) {
        this.noteJobRepository = noteJobRepository;
    }

    @Transactional
    public void tryPersist(Long userId, NoteResult result) {
        try {
            String fileName = extractMarkdownFileName(result.downloadUrl());
            if (fileName == null) {
                log.warn("Skipping note_job persist: invalid downloadUrl");
                return;
            }
            NoteJob row = new NoteJob();
            row.setUserId(userId);
            row.setNoteId(result.noteId());
            row.setMarkdownFileName(fileName);
            row.setSourceLabel(truncate(result.sourceFilename(), LABEL_MAX));
            row.setTitle(truncate(result.title(), LABEL_MAX));
            row.setAbstractExcerpt(NoteJob.truncateAbstract(result.abstractText()));
            noteJobRepository.save(row);
        } catch (Exception ex) {
            log.warn("Failed to persist note_job for user {} note {}", userId, result.noteId(), ex);
        }
    }

    private static String extractMarkdownFileName(String downloadUrl) {
        if (downloadUrl == null || !downloadUrl.startsWith(DOWNLOAD_PREFIX)) {
            return null;
        }
        String rest = downloadUrl.substring(DOWNLOAD_PREFIX.length());
        if (rest.isBlank() || rest.contains("/") || rest.contains("\\") || rest.contains("..")) {
            return null;
        }
        return rest;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
