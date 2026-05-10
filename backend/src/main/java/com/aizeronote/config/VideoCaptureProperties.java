package com.aizeronote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "video")
public record VideoCaptureProperties(
        String ytDlpPath,
        String ffmpegPath,
        String framesSubdir,
        Integer defaultMaxCount,
        Double defaultSceneThreshold,
        Integer defaultIntervalSec,
        Integer processTimeoutSeconds,
        Integer maxFrameCount,
        Double minSceneThreshold,
        Double maxSceneThreshold,
        Integer cleanupRetentionDays,
        String ytDlpCookiesFromBrowser,
        String ytDlpCookieFile
) {
}
