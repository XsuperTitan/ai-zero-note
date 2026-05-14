package com.aizeronote.service;

import com.aizeronote.model.dto.history.NoteJobDetailDto;
import com.aizeronote.model.dto.history.NoteJobSummaryDto;
import com.aizeronote.model.dto.history.VideoTaskDetailDto;
import com.aizeronote.model.dto.history.VideoTaskSummaryDto;
import com.aizeronote.model.entity.NoteJob;
import com.aizeronote.model.entity.VideoTask;
import com.aizeronote.repository.NoteJobRepository;
import com.aizeronote.repository.VideoTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TaskHistoryService {

    private static final String NOTE_DOWNLOAD_PREFIX = "/api/notes/download/";

    private final NoteJobRepository noteJobRepository;
    private final VideoTaskRepository videoTaskRepository;

    public TaskHistoryService(NoteJobRepository noteJobRepository, VideoTaskRepository videoTaskRepository) {
        this.noteJobRepository = noteJobRepository;
        this.videoTaskRepository = videoTaskRepository;
    }

    @Transactional(readOnly = true)
    public Page<NoteJobSummaryDto> listNotes(Long userId, Pageable pageable) {
        return noteJobRepository.findAllByUserId(userId, pageable).map(this::toNoteSummary);
    }

    @Transactional(readOnly = true)
    public Optional<NoteJobDetailDto> getNote(Long userId, String noteId) {
        return noteJobRepository.findByNoteIdAndUserId(noteId, userId).map(this::toNoteDetail);
    }

    @Transactional(readOnly = true)
    public Page<VideoTaskSummaryDto> listVideoTasks(Long userId, Pageable pageable) {
        return videoTaskRepository.findAllByUserId(userId, pageable).map(this::toVideoSummary);
    }

    @Transactional(readOnly = true)
    public Optional<VideoTaskDetailDto> getVideoTask(Long userId, String taskId) {
        return videoTaskRepository.findByTaskIdAndUserId(taskId, userId).map(this::toVideoDetail);
    }

    private NoteJobSummaryDto toNoteSummary(NoteJob row) {
        return new NoteJobSummaryDto(
                row.getNoteId(),
                row.getSourceLabel(),
                row.getTitle(),
                row.getAbstractExcerpt(),
                NOTE_DOWNLOAD_PREFIX + row.getMarkdownFileName(),
                row.getCreatedAt()
        );
    }

    private NoteJobDetailDto toNoteDetail(NoteJob row) {
        return new NoteJobDetailDto(
                row.getNoteId(),
                row.getSourceLabel(),
                row.getTitle(),
                row.getAbstractExcerpt(),
                row.getMarkdownFileName(),
                NOTE_DOWNLOAD_PREFIX + row.getMarkdownFileName(),
                row.getCreatedAt()
        );
    }

    private VideoTaskSummaryDto toVideoSummary(VideoTask row) {
        return new VideoTaskSummaryDto(
                row.getTaskId(),
                row.getSourceUrl(),
                row.getTitleSnapshot(),
                row.getDurationSeconds(),
                row.getFrameCount(),
                row.getCreatedAt()
        );
    }

    private VideoTaskDetailDto toVideoDetail(VideoTask row) {
        return new VideoTaskDetailDto(
                row.getTaskId(),
                row.getSourceUrl(),
                row.getTitleSnapshot(),
                row.getDurationSeconds(),
                row.getFrameCount(),
                row.getCreatedAt()
        );
    }
}
