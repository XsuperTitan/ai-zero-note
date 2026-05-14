package com.aizeronote.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "video_task")
public class VideoTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id", nullable = false, unique = true, length = 8)
    private String taskId;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "title_snapshot", length = 512)
    private String titleSnapshot;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "frame_count", nullable = false)
    private Integer frameCount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public void setTitleSnapshot(String titleSnapshot) {
        this.titleSnapshot = titleSnapshot;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(Integer frameCount) {
        this.frameCount = frameCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
