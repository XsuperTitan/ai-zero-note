package com.aizeronote.service.guidance;

import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.dto.guidance.StudyPlanGenerateRequest;
import com.aizeronote.model.dto.guidance.StudyPlanResponse;
import com.aizeronote.model.dto.guidance.StudyPlanVideoDto;
import com.aizeronote.model.entity.GuidanceSession;
import com.aizeronote.model.entity.GuidanceStudyPlan;
import com.aizeronote.model.guidance.GuidanceSessionStatus;
import com.aizeronote.model.guidance.StudyPlanGenerationMode;
import com.aizeronote.model.guidance.StudyPlanGenerationSource;
import com.aizeronote.repository.GuidanceSessionRepository;
import com.aizeronote.repository.GuidanceStudyPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class GuidanceStudyPlanService {

    private static final Logger log = LoggerFactory.getLogger(GuidanceStudyPlanService.class);

    private final GuidanceSessionRepository guidanceSessionRepository;
    private final GuidanceStudyPlanRepository guidanceStudyPlanRepository;
    private final ObjectMapper objectMapper;
    private final TemplateStudyPlanGenerator templateStudyPlanGenerator;
    private final StudyPlanLlmGenerator studyPlanLlmGenerator;

    public GuidanceStudyPlanService(
            GuidanceSessionRepository guidanceSessionRepository,
            GuidanceStudyPlanRepository guidanceStudyPlanRepository,
            ObjectMapper objectMapper,
            TemplateStudyPlanGenerator templateStudyPlanGenerator,
            StudyPlanLlmGenerator studyPlanLlmGenerator
    ) {
        this.guidanceSessionRepository = guidanceSessionRepository;
        this.guidanceStudyPlanRepository = guidanceStudyPlanRepository;
        this.objectMapper = objectMapper;
        this.templateStudyPlanGenerator = templateStudyPlanGenerator;
        this.studyPlanLlmGenerator = studyPlanLlmGenerator;
    }

    @Transactional
    public StudyPlanResponse generateStudyPlan(Long userId, StudyPlanGenerateRequest request) {
        GuidanceSession session = guidanceSessionRepository
                .findByIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "导学记录不存在或无权访问"));
        LearningQuestionnaireSubmitRequest questionnaire = readQuestionnaire(session);
        StudyPlanGenerationMode mode = request.getMode() == null
                ? StudyPlanGenerationMode.TEMPLATE
                : request.getMode();

        String source = StudyPlanGenerationSource.TEMPLATE;
        StudyPlanPayload payload;
        if (mode == StudyPlanGenerationMode.LLM) {
            try {
                payload = studyPlanLlmGenerator.generate(
                        questionnaire,
                        session.getReportSummary(),
                        session.getLlmPromptConstraints()
                );
                source = StudyPlanGenerationSource.LLM;
            } catch (Exception ex) {
                log.warn("Study plan LLM generation failed, using template", ex);
                payload = templateStudyPlanGenerator.generate(questionnaire, session.getReportSummary());
                source = StudyPlanGenerationSource.LLM_FALLBACK;
            }
        } else {
            payload = templateStudyPlanGenerator.generate(questionnaire, session.getReportSummary());
        }

        StudyPlanPayload sanitized = sanitizeAndNormalize(payload);
        GuidanceStudyPlan saved = persistPlan(session, sanitized, source);
        session.setStatus(GuidanceSessionStatus.PLAN_READY);
        guidanceSessionRepository.save(session);
        return toResponse(session.getId(), sanitized, source, saved);
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse getBySessionId(Long userId, Long sessionId) {
        GuidanceSession session = guidanceSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "导学记录不存在或无权访问"));
        GuidanceStudyPlan plan = guidanceStudyPlanRepository
                .findBySession_Id(session.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR, "该会话暂无学习方案，请先生成"));
        StudyPlanPayload payload = readPlanPayload(plan.getPlanJson());
        return toResponse(session.getId(), payload, plan.getGenerationSource(), plan);
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse getLatest(Long userId) {
        GuidanceStudyPlan plan = guidanceStudyPlanRepository
                .findFirstBySessionUserIdOrderBySessionCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR, "暂无已生成的学习方案"));
        GuidanceSession session = plan.getSession();
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "导学记录不存在或无权访问");
        }
        StudyPlanPayload payload = readPlanPayload(plan.getPlanJson());
        return toResponse(session.getId(), payload, plan.getGenerationSource(), plan);
    }

    private LearningQuestionnaireSubmitRequest readQuestionnaire(GuidanceSession session) {
        try {
            return objectMapper.readValue(session.getQuestionnaireJson(), LearningQuestionnaireSubmitRequest.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "问卷解析失败");
        }
    }

    private StudyPlanPayload readPlanPayload(String json) {
        try {
            return objectMapper.readValue(json, StudyPlanPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "学习方案解析失败");
        }
    }

    private GuidanceStudyPlan persistPlan(GuidanceSession session, StudyPlanPayload payload, String source) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "学习方案序列化失败");
        }
        GuidanceStudyPlan plan = guidanceStudyPlanRepository
                .findBySession_Id(session.getId())
                .orElseGet(GuidanceStudyPlan::new);
        plan.setSession(session);
        plan.setPlanJson(json);
        plan.setGenerationSource(source);
        return guidanceStudyPlanRepository.saveAndFlush(plan);
    }

    private static StudyPlanPayload sanitizeAndNormalize(StudyPlanPayload raw) {
        List<StudyPlanVideoPayload> videos = new ArrayList<>();
        for (StudyPlanVideoPayload v : raw.videos()) {
            String safeUrl = StudyPlanUrlSanitizer.sanitizeOrBlank(v.url());
            String kind = v.linkKind();
            if (!StringUtils.hasText(safeUrl)) {
                kind = "SEARCH_PLACEHOLDER";
            }
            videos.add(new StudyPlanVideoPayload(
                    v.id(),
                    v.title(),
                    v.platform(),
                    safeUrl,
                    v.rationale(),
                    v.sortOrder(),
                    kind
            ));
        }
        videos.sort(Comparator.comparingInt(StudyPlanVideoPayload::sortOrder));
        Set<String> ids = new HashSet<>();
        for (StudyPlanVideoPayload v : videos) {
            ids.add(v.id());
        }
        String current = raw.currentVideoId();
        if (!StringUtils.hasText(current) || !ids.contains(current)) {
            current = videos.isEmpty() ? "" : videos.get(0).id();
        }
        return new StudyPlanPayload(
                StringUtils.hasText(raw.outlineMarkdown()) ? raw.outlineMarkdown() : "（大纲暂空）",
                raw.suggestions() == null ? List.of() : raw.suggestions(),
                raw.priorities() == null ? List.of() : raw.priorities(),
                videos,
                current
        );
    }

    private StudyPlanResponse toResponse(
            long sessionId,
            StudyPlanPayload payload,
            String generationSource,
            GuidanceStudyPlan entity
    ) {
        List<StudyPlanVideoDto> videoDtos = payload.videos().stream()
                .map(v -> new StudyPlanVideoDto(
                        v.id(),
                        v.title(),
                        v.platform(),
                        v.url(),
                        v.rationale(),
                        v.sortOrder(),
                        v.linkKind()
                ))
                .toList();
        return new StudyPlanResponse(
                sessionId,
                generationSource,
                payload.outlineMarkdown(),
                payload.suggestions(),
                payload.priorities(),
                videoDtos,
                payload.currentVideoId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
