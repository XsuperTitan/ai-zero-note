package com.aizeronote.service;

import com.aizeronote.model.VideoFrameResult;
import com.aizeronote.model.VideoMetaResult;
import com.aizeronote.model.entity.VideoTask;
import com.aizeronote.repository.VideoTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoTaskService {

    private static final Logger log = LoggerFactory.getLogger(VideoTaskService.class);
    private static final int SOURCE_URL_MAX = 2048;
    private static final int TITLE_MAX = 512;

    private final VideoTaskRepository videoTaskRepository;

    public VideoTaskService(VideoTaskRepository videoTaskRepository) {
        this.videoTaskRepository = videoTaskRepository;
    }

    @Transactional
    public void tryPersist(Long userId, String requestUrl, VideoFrameResult result) {
        try {
            VideoTask row = new VideoTask();
            row.setUserId(userId);
            row.setTaskId(result.taskId());
            row.setSourceUrl(truncateUrl(requestUrl));
            VideoMetaResult meta = result.meta();
            if (meta != null) {
                row.setTitleSnapshot(truncate(meta.title(), TITLE_MAX));
                row.setDurationSeconds(toIntSeconds(meta.durationSeconds()));
            }
            row.setFrameCount(result.frames().size());
            videoTaskRepository.save(row);
        } catch (Exception ex) {
            log.warn("Failed to persist video_task for user {} task {}", userId, result.taskId(), ex);
        }
    }

    private static String truncateUrl(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        return u.length() <= SOURCE_URL_MAX ? u : u.substring(0, SOURCE_URL_MAX);
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private static Integer toIntSeconds(long durationSeconds) {
        if (durationSeconds <= 0) {
            return 0;
        }
        if (durationSeconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) durationSeconds;
    }
}
