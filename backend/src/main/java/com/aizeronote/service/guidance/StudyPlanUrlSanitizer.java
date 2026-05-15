package com.aizeronote.service.guidance;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class StudyPlanUrlSanitizer {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "search.bilibili.com",
            "www.bilibili.com",
            "bilibili.com",
            "m.bilibili.com",
            "www.youtube.com",
            "youtube.com",
            "m.youtube.com",
            "youtu.be",
            "www.youtube-nocookie.com"
    );

    private StudyPlanUrlSanitizer() {
    }

    public static String sanitizeOrBlank(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        String trimmed = rawUrl.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            return "";
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if (host == null) {
                return "";
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            if (ALLOWED_HOSTS.contains(normalized)) {
                return trimmed;
            }
            return "";
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
