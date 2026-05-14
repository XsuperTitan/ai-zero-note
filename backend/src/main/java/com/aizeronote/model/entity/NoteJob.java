package com.aizeronote.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "note_job")
public class NoteJob {

    private static final int ABSTRACT_MAX_LEN = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "note_id", nullable = false, unique = true, length = 36)
    private String noteId;

    @Column(name = "markdown_file_name", nullable = false, length = 512)
    private String markdownFileName;

    @Column(name = "source_label", length = 512)
    private String sourceLabel;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "abstract_excerpt", length = 2000)
    private String abstractExcerpt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public static String truncateAbstract(String abstractText) {
        if (abstractText == null) {
            return null;
        }
        return abstractText.length() <= ABSTRACT_MAX_LEN
                ? abstractText
                : abstractText.substring(0, ABSTRACT_MAX_LEN);
    }

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

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getMarkdownFileName() {
        return markdownFileName;
    }

    public void setMarkdownFileName(String markdownFileName) {
        this.markdownFileName = markdownFileName;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractExcerpt() {
        return abstractExcerpt;
    }

    public void setAbstractExcerpt(String abstractExcerpt) {
        this.abstractExcerpt = abstractExcerpt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
