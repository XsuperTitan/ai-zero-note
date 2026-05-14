package com.aizeronote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "image-notes")
public record ImageNotesProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("openai") String provider,
        /** OpenAI 兼容网关根路径，形如 https://api.openai.com/v1（仅 provider=openai 使用） */
        String baseUrl,
        /** DashScope API 根路径，形如 https://dashscope.aliyuncs.com/api/v1（仅 provider=wanx 使用） */
        String wanBaseUrl,
        String apiKey,
        String model,
        String size,
        int maxSourceChars
) {
}
