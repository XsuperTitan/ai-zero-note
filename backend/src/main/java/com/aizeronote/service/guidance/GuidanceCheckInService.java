package com.aizeronote.service.guidance;

import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.dto.guidance.GuidanceCheckInCreateRequest;
import com.aizeronote.model.dto.guidance.GuidanceCheckInResponse;
import com.aizeronote.model.dto.guidance.GuidanceCheckInSupplementRequest;
import com.aizeronote.model.entity.GuidanceCheckIn;
import com.aizeronote.model.entity.GuidanceSession;
import com.aizeronote.repository.GuidanceCheckInRepository;
import com.aizeronote.repository.GuidanceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GuidanceCheckInService {

    private final GuidanceSessionRepository guidanceSessionRepository;
    private final GuidanceCheckInRepository guidanceCheckInRepository;

    public GuidanceCheckInService(
            GuidanceSessionRepository guidanceSessionRepository,
            GuidanceCheckInRepository guidanceCheckInRepository
    ) {
        this.guidanceSessionRepository = guidanceSessionRepository;
        this.guidanceCheckInRepository = guidanceCheckInRepository;
    }

    @Transactional
    public GuidanceCheckInResponse create(Long userId, GuidanceCheckInCreateRequest request) {
        GuidanceSession session = guidanceSessionRepository
                .findByIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "导学记录不存在或无权访问"));
        GuidanceCheckIn row = new GuidanceCheckIn();
        row.setUserId(userId);
        row.setSession(session);
        row.setRemark(trimToNull(request.getRemark()));
        GuidanceCheckIn saved = guidanceCheckInRepository.save(row);
        return toResponse(saved);
    }

    @Transactional
    public GuidanceCheckInResponse addSupplement(Long userId, Long checkInId, GuidanceCheckInSupplementRequest request) {
        GuidanceCheckIn row = loadOwned(checkInId, userId);
        if (request.getVideoUrl() != null) {
            String v = request.getVideoUrl().trim();
            row.setVideoUrl(v.isEmpty() ? null : v);
        }
        if (request.getTranscriptText() != null) {
            String t = request.getTranscriptText().trim();
            row.setTranscriptText(t.isEmpty() ? null : t);
        }
        GuidanceCheckIn saved = guidanceCheckInRepository.save(row);
        return toResponse(saved);
    }

    /**
     * Supplemental text for the mixed note pipeline; empty if {@code checkInId} is null.
     *
     * @throws IllegalArgumentException if id is non-null but not found for user
     */
    @Transactional(readOnly = true)
    public String supplementalBlockForNotePipeline(Long userId, Long checkInId) {
        if (checkInId == null) {
            return "";
        }
        GuidanceCheckIn row = guidanceCheckInRepository
                .findByIdAndUserId(checkInId, userId)
                .orElseThrow(() -> new IllegalArgumentException("打卡记录不存在或无权使用"));
        String block = GuidanceCheckInPrompts.formatCheckInSupplement(row);
        if (!StringUtils.hasText(block)) {
            throw new IllegalArgumentException("打卡记录无任何备注/链接/转写，无法作为笔记素材");
        }
        return block;
    }

    @Transactional
    public void markConsumedByNote(Long userId, Long checkInId, String noteId) {
        if (checkInId == null || noteId == null || noteId.isBlank()) {
            return;
        }
        guidanceCheckInRepository.findByIdAndUserId(checkInId, userId).ifPresent(row -> {
            row.setConsumedNoteId(noteId.trim());
            guidanceCheckInRepository.save(row);
        });
    }

    private GuidanceCheckIn loadOwned(Long checkInId, Long userId) {
        return guidanceCheckInRepository
                .findByIdAndUserId(checkInId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "打卡记录不存在或无权访问"));
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static GuidanceCheckInResponse toResponse(GuidanceCheckIn row) {
        return new GuidanceCheckInResponse(
                row.getId(),
                row.getSession().getId(),
                row.getRemark(),
                row.getVideoUrl(),
                StringUtils.hasText(row.getTranscriptText()),
                row.getConsumedNoteId(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
