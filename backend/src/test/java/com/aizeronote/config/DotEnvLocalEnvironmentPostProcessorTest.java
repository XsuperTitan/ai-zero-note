package com.aizeronote.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvLocalEnvironmentPostProcessorTest {

    @Test
    void addConfigurationPropertyAliases_mirrorsImageNotesEnabledIntoCanonicalPrefix() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("IMAGE_NOTES_ENABLED", "true");
        map.put("IMAGE_NOTES_PROVIDER", "wanx");

        DotEnvLocalEnvironmentPostProcessor.addConfigurationPropertyAliases(map);

        assertThat(map.get("image-notes.enabled")).isEqualTo("true");
        assertThat(map.get("image-notes.provider")).isEqualTo("wanx");
    }

    @Test
    void parse_stripsTrailingHashCommentUnlessValueIsQuoted() {
        Map<String, Object> parsed = DotEnvLocalEnvironmentPostProcessor.parse(
                List.of(
                        "IMAGE_NOTES_ENABLED=true # ok",
                        "KEEP=\"literal # keeps\""
                ));

        assertThat(parsed.get("IMAGE_NOTES_ENABLED")).isEqualTo("true");
        assertThat(parsed.get("KEEP")).isEqualTo("literal # keeps");
    }
}
