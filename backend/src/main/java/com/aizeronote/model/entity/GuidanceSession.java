package com.aizeronote.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "guidance_session")
public class GuidanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tutor_persona", nullable = false, length = 32)
    private String tutorPersona;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "questionnaire_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String questionnaireJson;

    @Column(name = "report_summary", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String reportSummary;

    @Column(name = "llm_prompt_constraints", columnDefinition = "MEDIUMTEXT")
    private String llmPromptConstraints;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

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

    public String getTutorPersona() {
        return tutorPersona;
    }

    public void setTutorPersona(String tutorPersona) {
        this.tutorPersona = tutorPersona;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuestionnaireJson() {
        return questionnaireJson;
    }

    public void setQuestionnaireJson(String questionnaireJson) {
        this.questionnaireJson = questionnaireJson;
    }

    public String getReportSummary() {
        return reportSummary;
    }

    public void setReportSummary(String reportSummary) {
        this.reportSummary = reportSummary;
    }

    public String getLlmPromptConstraints() {
        return llmPromptConstraints;
    }

    public void setLlmPromptConstraints(String llmPromptConstraints) {
        this.llmPromptConstraints = llmPromptConstraints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
