package com.aizeronote.model.dto.guidance;

import com.aizeronote.model.guidance.ContentPreference;
import com.aizeronote.model.guidance.LearningUrgency;
import com.aizeronote.model.guidance.TutorPersona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LearningQuestionnaireSubmitRequest {

    @NotNull
    private TutorPersona tutorPersona;

    @NotBlank
    @Size(max = 512)
    private String subjectOrTopic;

    @NotNull
    private LearningUrgency urgency;

    @NotBlank
    @Size(max = 512)
    private String preferredPlatforms;

    @NotBlank
    @Size(max = 512)
    private String usualSites;

    @NotBlank
    @Size(max = 512)
    private String studyRhythm;

    @NotNull
    private ContentPreference contentPreference;

    @Size(max = 2000)
    private String extraNotes;

    public TutorPersona getTutorPersona() {
        return tutorPersona;
    }

    public void setTutorPersona(TutorPersona tutorPersona) {
        this.tutorPersona = tutorPersona;
    }

    public String getSubjectOrTopic() {
        return subjectOrTopic;
    }

    public void setSubjectOrTopic(String subjectOrTopic) {
        this.subjectOrTopic = subjectOrTopic;
    }

    public LearningUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(LearningUrgency urgency) {
        this.urgency = urgency;
    }

    public String getPreferredPlatforms() {
        return preferredPlatforms;
    }

    public void setPreferredPlatforms(String preferredPlatforms) {
        this.preferredPlatforms = preferredPlatforms;
    }

    public String getUsualSites() {
        return usualSites;
    }

    public void setUsualSites(String usualSites) {
        this.usualSites = usualSites;
    }

    public String getStudyRhythm() {
        return studyRhythm;
    }

    public void setStudyRhythm(String studyRhythm) {
        this.studyRhythm = studyRhythm;
    }

    public ContentPreference getContentPreference() {
        return contentPreference;
    }

    public void setContentPreference(ContentPreference contentPreference) {
        this.contentPreference = contentPreference;
    }

    public String getExtraNotes() {
        return extraNotes;
    }

    public void setExtraNotes(String extraNotes) {
        this.extraNotes = extraNotes;
    }
}
