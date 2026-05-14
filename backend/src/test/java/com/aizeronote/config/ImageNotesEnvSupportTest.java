package com.aizeronote.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageNotesEnvSupportTest {

    @Test
    void yamlPlaceholder_imageNotesEnabled_false_isNotCountedAsExplicitUserOptOut() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("image-notes.enabled", "false");

        ImageNotesProperties props = new ImageNotesProperties(
                false,
                "openai",
                "https://api.openai.com/v1",
                "https://dashscope.aliyuncs.com/api/v1",
                null,
                null,
                "",
                12000);

        ImageNotesEnvSupport.ImageNotesGateDecision gate = ImageNotesEnvSupport.evaluateGate(env, props);
        assertThat(gate.explicitOptOut()).isFalse();
    }

    @Test
    void parsedMap_acceptsBomPrefixedCanonicalKey_asTrue() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("\ufeffimage-notes.enabled", "true");

        assertThat(ImageNotesEnvSupport.evaluateParsedEnvMapForEnable(m)).isTrue();
    }

    @Test
    void parsedMap_acceptsImageNoteEnabledTypo_YES() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("IMAGE_NOTE_ENABLED", "YES");

        assertThat(ImageNotesEnvSupport.evaluateParsedEnvMapForEnable(m)).isTrue();
    }

    @Test
    void parsedMap_implicitWanProviderAndVisionKey_withoutExplicitEnableFlag_enables() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("IMAGE_NOTES_PROVIDER", "wanx");
        m.put("VISION_API_KEY", "sk-test-placeholder");

        assertThat(ImageNotesEnvSupport.evaluateParsedEnvMapForEnable(m)).isTrue();
    }

    @Test
    void parsedMap_explicitFalse_disablesDespiteWanStack() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("IMAGE_NOTES_ENABLED", "false");
        m.put("IMAGE_NOTES_PROVIDER", "wanx");
        m.put("VISION_API_KEY", "sk-test-placeholder");

        assertThat(ImageNotesEnvSupport.evaluateParsedEnvMapForEnable(m)).isFalse();
    }
}
