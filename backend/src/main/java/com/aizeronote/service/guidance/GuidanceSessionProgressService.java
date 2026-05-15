package com.aizeronote.service.guidance;

import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.dto.guidance.GuidanceActiveProgressResponse;
import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.entity.GuidanceSession;
import com.aizeronote.model.entity.GuidanceStudyPlan;
import com.aizeronote.model.guidance.GuidanceSessionStatus;
import com.aizeronote.repository.GuidanceSessionRepository;
import com.aizeronote.repository.GuidanceStudyPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GuidanceSessionProgressService {

    private final GuidanceSessionRepository guidanceSessionRepository;
    private final GuidanceStudyPlanRepository guidanceStudyPlanRepository;
    private final ObjectMapper objectMapper;

    public GuidanceSessionProgressService(
            GuidanceSessionRepository guidanceSessionRepository,
            GuidanceStudyPlanRepository guidanceStudyPlanRepository,
            ObjectMapper objectMapper
    ) {
        this.guidanceSessionRepository = guidanceSessionRepository;
        this.guidanceStudyPlanRepository = guidanceStudyPlanRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enterInProgress(Long userId, Long sessionId) {
        GuidanceSession session = loadOwned(sessionId, userId);
        if (guidanceStudyPlanRepository.findBySession_Id(session.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先生成学习方案");
        }
        if (GuidanceSessionStatus.COMPLETED.equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该导学已标记完成");
        }
        if (GuidanceSessionStatus.PROFILED.equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先生成学习方案");
        }
        if (GuidanceSessionStatus.PLAN_READY.equals(session.getStatus())) {
            session.setStatus(GuidanceSessionStatus.IN_PROGRESS);
            guidanceSessionRepository.save(session);
        }
    }

    @Transactional
    public void updateCurrentVideo(Long userId, Long sessionId, String currentVideoId) {
        GuidanceSession session = loadOwned(sessionId, userId);
        if (GuidanceSessionStatus.COMPLETED.equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该导学已标记完成，无法切换视频");
        }
        GuidanceStudyPlan plan = guidanceStudyPlanRepository
                .findBySession_Id(session.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR, "请先生成学习方案"));
        StudyPlanPayload payload = readPlanPayload(plan.getPlanJson());
        String vid = currentVideoId == null ? "" : currentVideoId.trim();
        boolean ok = payload.videos().stream().anyMatch(v -> v.id().equals(vid));
        if (!ok) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的视频编号");
        }
        session.setProgressVideoId(vid);
        if (GuidanceSessionStatus.PLAN_READY.equals(session.getStatus())) {
            session.setStatus(GuidanceSessionStatus.IN_PROGRESS);
        }
        guidanceSessionRepository.save(session);
    }

    @Transactional
    public void complete(Long userId, Long sessionId) {
        GuidanceSession session = loadOwned(sessionId, userId);
        if (GuidanceSessionStatus.COMPLETED.equals(session.getStatus())) {
            return;
        }
        if (!GuidanceSessionStatus.PLAN_READY.equals(session.getStatus())
                && !GuidanceSessionStatus.IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前状态无法标记完成");
        }
        session.setStatus(GuidanceSessionStatus.COMPLETED);
        guidanceSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<GuidanceActiveProgressResponse> findActiveProgress(Long userId) {
        List<String> statuses = List.of(GuidanceSessionStatus.PLAN_READY, GuidanceSessionStatus.IN_PROGRESS);
        List<GuidanceSession> candidates = guidanceSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(
                userId,
                statuses,
                PageRequest.of(0, 12)
        );
        for (GuidanceSession s : candidates) {
            Optional<GuidanceStudyPlan> planOpt = guidanceStudyPlanRepository.findBySession_Id(s.getId());
            if (planOpt.isEmpty()) {
                continue;
            }
            StudyPlanPayload payload;
            try {
                payload = objectMapper.readValue(planOpt.get().getPlanJson(), StudyPlanPayload.class);
            } catch (JsonProcessingException e) {
                continue;
            }
            LearningQuestionnaireSubmitRequest q;
            try {
                q = objectMapper.readValue(s.getQuestionnaireJson(), LearningQuestionnaireSubmitRequest.class);
            } catch (JsonProcessingException e) {
                continue;
            }
            String effective = GuidanceStudyPlanService.resolveEffectiveCurrentVideoId(s, payload);
            String title = payload.videos().stream()
                    .filter(v -> v.id().equals(effective))
                    .map(StudyPlanVideoPayload::title)
                    .findFirst()
                    .orElse("");
            String path = "/guidance/plan?sessionId=" + s.getId();
            return Optional.of(new GuidanceActiveProgressResponse(
                    s.getId(),
                    s.getStatus(),
                    s.getTutorPersona(),
                    q.getSubjectOrTopic(),
                    effective,
                    title,
                    path
            ));
        }
        return Optional.empty();
    }

    private GuidanceSession loadOwned(Long sessionId, Long userId) {
        return guidanceSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "导学记录不存在或无权访问"));
    }

    private StudyPlanPayload readPlanPayload(String json) {
        try {
            return objectMapper.readValue(json, StudyPlanPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "学习方案解析失败");
        }
    }
}
