package com.aizeronote.service;

import com.aizeronote.config.ImageNotesEnvSupport;
import com.aizeronote.config.ImageNotesProperties;
import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.ImageNoteJobStatus;
import com.aizeronote.model.dto.ImageNoteEnqueueResponse;
import com.aizeronote.model.dto.ImageNoteStatusResponse;
import com.aizeronote.model.entity.ImageNoteJob;
import com.aizeronote.repository.ImageNoteJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImageNoteJobService {

    private static final Logger log = LoggerFactory.getLogger(ImageNoteJobService.class);

    public static final Pattern SAFE_IMAGE_FILENAME = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.png$"
    );

    private final ImageNotesProperties props;
    private final Environment environment;
    private final ImageNoteJobRepository repository;
    private final ImageNoteAsyncDispatcher asyncDispatcher;

    public ImageNoteJobService(
            ImageNotesProperties props,
            Environment environment,
            ImageNoteJobRepository repository,
            ImageNoteAsyncDispatcher asyncDispatcher
    ) {
        this.props = props;
        this.environment = environment;
        this.repository = repository;
        this.asyncDispatcher = asyncDispatcher;
    }

    @Transactional
    public ImageNoteEnqueueResponse enqueue(Long userId, String rawSourceText) {
        ensureEnabledConfigured();
        if (!StringUtils.hasText(rawSourceText)) {
            throw new IllegalArgumentException("sourceText is required.");
        }
        String trimmed = rawSourceText.trim();
        String excerpt = truncate(trimmed, effectiveMaxChars(props.maxSourceChars()));

        ImageNoteJob job = new ImageNoteJob();
        String jobId = UUID.randomUUID().toString();
        job.setJobId(jobId);
        job.setUserId(userId);
        job.setStatus(ImageNoteJobStatus.PENDING.name());
        job.setSourceExcerpt(excerpt);
        repository.saveAndFlush(job);
        asyncDispatcher.dispatch(jobId);
        return new ImageNoteEnqueueResponse(jobId, ImageNoteJobStatus.PENDING.name());
    }

    public Optional<ImageNoteStatusResponse> status(Long userId, String jobId) {
        return repository.findByJobIdAndUserId(jobId, userId).map(this::toResponse);
    }

    public boolean ownsImage(Long userId, String fileName) {
        return repository.findByImageFileNameAndUserId(fileName, userId).isPresent();
    }

    private ImageNoteStatusResponse toResponse(ImageNoteJob job) {
        String url = "";
        if (ImageNoteJobStatus.SUCCEEDED.name().equals(job.getStatus())
                && StringUtils.hasText(job.getImageFileName())) {
            url = "/api/notes/image-download/" + job.getImageFileName();
        }
        String err = job.getErrorMessage() != null ? job.getErrorMessage() : "";
        return new ImageNoteStatusResponse(job.getJobId(), job.getStatus(), err, url);
    }

    public void ensureEnabledConfigured() {
        ImageNotesEnvSupport.ImageNotesGateDecision gate = ImageNotesEnvSupport.evaluateGate(environment, props);
        if (!gate.overallEnabled()) {
            log.warn(
                    "{} Typical causes (not upstream image API failures): explicitOptOut mis-detected from YAML default "
                            + "image-notes.enabled=false; unreadable .env.local vs user.dir; MapPropertySource not visible to "
                            + "getProperty; set flat IMAGE_NOTES_ENABLED=false only when you intend to disable. "
                            + "diskFlat.IMAGE_NOTES_*=false strongly suggests IMAGE_NOTES lines exist only in an unsaved editor buffer.",
                    gate.toDiagnosticLogLine()
            );
            String message = gate.likelyMissingImageNotesLinesOnDisk()
                    ? "Image notes are disabled. Save backend/.env.local to disk (IMAGE_NOTES_ENABLED / IMAGE_NOTES_PROVIDER were not read from file)."
                    : "Image notes are disabled.";
            throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
        }
        if (!StringUtils.hasText(ImageNotesEnvSupport.apiKey(environment, props))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Image notes API key is not configured.");
        }
        boolean wan = ImageNotesEnvSupport.isWan(environment, props);
        if (wan) {
            if (!StringUtils.hasText(trimToNull(ImageNotesEnvSupport.effectiveWanBaseUrl(environment, props)))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Image notes wan-base-url is not configured.");
            }
        } else {
            if (!StringUtils.hasText(trimToNull(ImageNotesEnvSupport.openAiBaseUrl(environment, props)))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Image notes base-url is not configured.");
            }
        }
    }

    private static String trimToNull(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : null;
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }

    private static int effectiveMaxChars(int configured) {
        return configured > 0 ? configured : 12000;
    }
}
