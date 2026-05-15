package com.aizeronote.service.guidance;

import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.dto.guidance.GuidanceProfileResponse;
import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.entity.GuidanceSession;
import com.aizeronote.model.guidance.GuidanceSessionStatus;
import com.aizeronote.repository.GuidanceSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningProfileService {

    private final GuidanceSessionRepository guidanceSessionRepository;
    private final ObjectMapper objectMapper;

    public LearningProfileService(GuidanceSessionRepository guidanceSessionRepository, ObjectMapper objectMapper) {
        this.guidanceSessionRepository = guidanceSessionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GuidanceProfileResponse submitQuestionnaire(Long userId, LearningQuestionnaireSubmitRequest request) {
        String questionnaireJson;
        try {
            questionnaireJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to serialize questionnaire");
        }
        String summary = LearningProfileReportBuilder.buildReportSummary(request);
        String constraints = LearningProfileReportBuilder.buildLlmPromptConstraints(request);

        GuidanceSession session = new GuidanceSession();
        session.setUserId(userId);
        session.setTutorPersona(request.getTutorPersona().name());
        session.setStatus(GuidanceSessionStatus.PROFILE_READY);
        session.setQuestionnaireJson(questionnaireJson);
        session.setReportSummary(summary);
        session.setLlmPromptConstraints(constraints);
        GuidanceSession saved = guidanceSessionRepository.save(session);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GuidanceProfileResponse getLatest(Long userId) {
        return guidanceSessionRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR, "尚未提交学习习惯问卷"));
    }

    @Transactional(readOnly = true)
    public GuidanceProfileResponse getById(Long userId, Long sessionId) {
        GuidanceSession session = guidanceSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "导学记录不存在或无权访问"));
        return toResponse(session);
    }

    private GuidanceProfileResponse toResponse(GuidanceSession session) {
        return new GuidanceProfileResponse(
                session.getId(),
                session.getTutorPersona(),
                session.getStatus(),
                session.getReportSummary(),
                session.getLlmPromptConstraints(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
