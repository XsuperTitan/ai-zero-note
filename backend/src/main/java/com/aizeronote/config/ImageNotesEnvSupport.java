package com.aizeronote.config;

import org.springframework.lang.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves image-notes settings from both {@link ImageNotesProperties} and raw {@link Environment}
 * keys (e.g. {@code IMAGE_NOTES_ENABLED} in a {@code MapPropertySource} from {@code .env.local}),
 * which may not bind into the {@link ImageNotesProperties} record.
 * <p>
 * Notes: {@code IMAGE_NOTES_MODEL} / {@code VISION_MODEL} only affect outbound API calls once the gate
 * passes — they cannot produce {@code Image notes are disabled.}; that string is emitted only when
 * {@link #enabled(Environment, ImageNotesProperties)} is false.<br>
 * This gate runs <strong>before</strong> any WAN / OpenAI image-generation HTTP request; failures here are
 * configuration issues, not external API outages.</p>
 */
public final class ImageNotesEnvSupport {

    static final String DEFAULT_WAN_IMAGE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";

    private ImageNotesEnvSupport() {
    }

    /**
     * Single evaluation used for logging and {@link #enabled}; avoids duplicate logic and double disk probes.
     */
    public record ImageNotesGateDecision(
            boolean overallEnabled,
            /** Flat {@code IMAGE_NOTES_ENABLED} set to falsy — not YAML {@code image-notes.enabled} default. */
            boolean explicitOptOut,
            /** {@code Environment} resolves {@code IMAGE_NOTES_ENABLED} / {@code image-notes.enabled} as on. */
            boolean environmentKeysShowOn,
            /** Raw OS env {@code IMAGE_NOTES_ENABLED}. */
            boolean osEnvironmentShowsOn,
            /** {@link ImageNotesProperties#enabled()} from bound configuration. */
            boolean configurationRecordEnabled,
            /** {@code wanx} provider + usable key chain (VISION / IMAGE_NOTES_API_KEY.) */
            boolean implicitWanConfigured,
            @Nullable Path dotEnvResolvedAbsolutePath,
            boolean dotEnvMergeOutcomeKnown,
            @Nullable Boolean dotEnvMergedEnableOutcome,
            @Nullable String dotEnvProbeIssue,
            /** Non-secret: provider token only (truncated.) */
            String providerResolvedSnippet,
            boolean wanProviderChosen,
            boolean apiKeyConfigured,
            /** {@code wan} branch only — {@code false} usually means DashScope URL comes from YAML default. */
            boolean wanBaseUrlExplicitlyResolved,
            /** Parsed from on-disk {@code .env.local} keys (literal), not IDE buffer. */
            boolean dotEnvDiskHasImageNotesEnabledKey,
            boolean dotEnvDiskHasImageNotesProviderKey
    ) {
        /** Single WARN line — no secrets, no API responses (gate runs before outbound image calls.) */
        public String toDiagnosticLogLine() {
            String pathStr = dotEnvResolvedAbsolutePath == null ? "-" : dotEnvResolvedAbsolutePath.toAbsolutePath().normalize().toString();
            String merged = dotEnvMergedEnableOutcome == null ? "-" : String.valueOf(dotEnvMergedEnableOutcome);
            String issue = dotEnvProbeIssue == null ? "-" : dotEnvProbeIssue;
            return String.format(
                    Locale.ROOT,
                    "[image-notes GATE] enabled=%s (before any WAN/OpenAI image HTTP) user.dir=%s "
                            + "explicitOptOut=%s envKeysOn=%s getenvOn=%s props.enabled=%s implicitWan=%s "
                            + "dotEnv[path=%s,merged=%s,mergeResolved=%s,issue=%s,diskFlat.IMAGE_NOTES_ENABLED=%s,diskFlat.IMAGE_NOTES_PROVIDER=%s] "
                            + "provider=%s isWan=%s apiKey=%s wanBaseExplicit=%s",
                    overallEnabled,
                    System.getProperty("user.dir", ""),
                    explicitOptOut,
                    environmentKeysShowOn,
                    osEnvironmentShowsOn,
                    configurationRecordEnabled,
                    implicitWanConfigured,
                    pathStr,
                    merged,
                    dotEnvMergeOutcomeKnown,
                    issue,
                    dotEnvDiskHasImageNotesEnabledKey,
                    dotEnvDiskHasImageNotesProviderKey,
                    providerResolvedSnippet.isEmpty() ? "-" : truncate(providerResolvedSnippet, 48),
                    wanProviderChosen,
                    apiKeyConfigured,
                    wanBaseUrlExplicitlyResolved
            );
        }

        private static String truncate(String s, int max) {
            String t = s.trim();
            if (t.length() <= max) {
                return t;
            }
            return t.substring(0, max) + "…";
        }

        /**
         * Heuristic: on-disk file was read but has no {@code IMAGE_NOTES_ENABLED} line while the gate stays off —
         * very often the editor tab has unsaved lines that never reached the JVM.
         */
        public boolean likelyMissingImageNotesLinesOnDisk() {
            return dotEnvResolvedAbsolutePath != null
                    && dotEnvMergeOutcomeKnown
                    && Boolean.FALSE.equals(dotEnvMergedEnableOutcome)
                    && !dotEnvDiskHasImageNotesEnabledKey
                    && !explicitOptOut;
        }
    }

    public static ImageNotesGateDecision evaluateGate(Environment env, ImageNotesProperties props) {
        boolean explicitOff = explicitTurnedOffViaEnvironmentOrOs(env);
        boolean envOn = envFlagTrue(env, "IMAGE_NOTES_ENABLED", "image-notes.enabled");
        boolean osOn = envFlagTrueFromRawEnv("IMAGE_NOTES_ENABLED");
        boolean propOn = props.enabled();
        boolean implicitWan = implicitWanConfigured(env, props);

        DiskGate disk = probeDiskGate();

        Boolean diskMerged = disk.mergedOutcome();
        boolean mergeKnown = diskMerged != null;
        boolean diskLayerPasses = mergeKnown && diskMerged;

        boolean overall = !explicitOff && (envOn || osOn || propOn || implicitWan || diskLayerPasses);

        String providerGuess = summarizeProvider(env, props);
        boolean wan = isWan(env, props);
        boolean api = StringUtils.hasText(apiKey(env, props));
        boolean wanBaseExplicit = wan && StringUtils.hasText(wanBaseUrl(env, props));

        return new ImageNotesGateDecision(
                overall,
                explicitOff,
                envOn,
                osOn,
                propOn,
                implicitWan,
                disk.path(),
                mergeKnown,
                diskMerged,
                disk.failure(),
                providerGuess,
                wan,
                api,
                wanBaseExplicit,
                disk.hasFlatImageNotesEnabled(),
                disk.hasFlatImageNotesProvider()
        );
    }

    /** Internal disk probe outcome for logging and gate layering. */
    private record DiskGate(
            @Nullable Path path,
            @Nullable Boolean mergedOutcome,
            @Nullable String failure,
            boolean hasFlatImageNotesEnabled,
            boolean hasFlatImageNotesProvider
    ) {
    }

    private static DiskGate probeDiskGate() {
        Path p = DotEnvLocalEnvironmentPostProcessor.locateFirstReadableEnvLocal();
        if (p == null) {
            return new DiskGate(null, null, "not_found", false, false);
        }
        Path abs = p.toAbsolutePath().normalize();
        try {
            Map<String, Object> map = DotEnvLocalEnvironmentPostProcessor.loadParsedEnvLocal(abs);
            boolean merged = evaluateParsedEnvMapForEnable(map);
            return new DiskGate(
                    abs,
                    merged,
                    null,
                    map.containsKey("IMAGE_NOTES_ENABLED"),
                    map.containsKey("IMAGE_NOTES_PROVIDER"));
        } catch (IOException ex) {
            return new DiskGate(abs, null, ex.getClass().getSimpleName(), false, false);
        }
    }

    private static String summarizeProvider(Environment env, ImageNotesProperties props) {
        return firstNonBlankText(
                env.getProperty("IMAGE_NOTES_PROVIDER"),
                env.getProperty("image-notes.provider"),
                props.provider() != null ? props.provider() : ""
        );
    }

    public static boolean enabled(Environment env, ImageNotesProperties props) {
        return evaluateGate(env, props).overallEnabled();
    }

    /**
     * When {@code IMAGE_NOTES_ENABLED} is absent from resolved config but WAN stack is wired
     * ({@code wanx}, usable API key via image-notes / vision chain), treat image notes as on.
     * WAN base falls back to {@link #DEFAULT_WAN_IMAGE_BASE_URL} for this check and callers that build clients.
     * Opt-out stays {@code IMAGE_NOTES_ENABLED=false}.
     */
    private static boolean implicitWanConfigured(Environment env, ImageNotesProperties props) {
        return isWan(env, props) && StringUtils.hasText(apiKey(env, props));
    }

    /** WAN HTTP root; aligns with {@code application.yml} default when unset. */
    public static String effectiveWanBaseUrl(Environment env, ImageNotesProperties props) {
        String u = wanBaseUrl(env, props);
        return StringUtils.hasText(u) ? u.trim() : DEFAULT_WAN_IMAGE_BASE_URL;
    }

    public static boolean isWan(Environment env, ImageNotesProperties props) {
        String p = firstNonBlankText(
                env.getProperty("IMAGE_NOTES_PROVIDER"),
                env.getProperty("image-notes.provider")
        );
        if (!StringUtils.hasText(p)) {
            p = props.provider();
        }
        return p != null && "wanx".equalsIgnoreCase(p.trim());
    }

    public static String apiKey(Environment env, ImageNotesProperties props) {
        return firstNonBlankText(
                env.getProperty("IMAGE_NOTES_API_KEY"),
                env.getProperty("image-notes.api-key"),
                env.getProperty("VISION_API_KEY"),
                props.apiKey()
        );
    }

    public static String wanBaseUrl(Environment env, ImageNotesProperties props) {
        return firstNonBlankText(
                env.getProperty("IMAGE_NOTES_WAN_BASE_URL"),
                env.getProperty("image-notes.wan-base-url"),
                props.wanBaseUrl()
        );
    }

    public static String openAiBaseUrl(Environment env, ImageNotesProperties props) {
        return firstNonBlankText(
                env.getProperty("IMAGE_NOTES_BASE_URL"),
                env.getProperty("image-notes.base-url"),
                env.getProperty("OPENAI_BASE_URL"),
                props.baseUrl()
        );
    }

    public static String model(Environment env, ImageNotesProperties props) {
        return firstNonBlankText(
                env.getProperty("IMAGE_NOTES_MODEL"),
                env.getProperty("image-notes.model"),
                props.model()
        );
    }

    public static String size(Environment env, ImageNotesProperties props) {
        return firstNonBlankText(
                env.getProperty("IMAGE_NOTES_SIZE"),
                env.getProperty("image-notes.size"),
                props.size()
        );
    }

    /**
     * User opt-out only when they set the flat env key {@code IMAGE_NOTES_ENABLED} to a falsy value
     * (Environment or OS process env). Do <strong>not</strong> use {@code image-notes.enabled}: it is routinely
     * {@code false} from {@code application.yml}'s placeholder {@code ${IMAGE_NOTES_ENABLED:false}} — that is a
     * default binding artifact, not an explicit disable.
     */
    private static boolean explicitTurnedOffViaEnvironmentOrOs(Environment env) {
        if (envFlagFalse(env, "IMAGE_NOTES_ENABLED")) {
            return true;
        }
        try {
            String raw = System.getenv("IMAGE_NOTES_ENABLED");
            return StringUtils.hasText(raw) && isFalsyFlag(raw);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private static boolean envFlagTrue(Environment env, String... keys) {
        for (String key : keys) {
            Boolean boxed = env.getProperty(key, Boolean.class);
            if (Boolean.TRUE.equals(boxed)) {
                return true;
            }
            String raw = env.getProperty(key);
            if (StringUtils.hasText(raw) && isTruthyFlag(raw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean envFlagFalse(Environment env, String... keys) {
        for (String key : keys) {
            Boolean boxed = env.getProperty(key, Boolean.class);
            if (Boolean.FALSE.equals(boxed)) {
                return true;
            }
            String raw = env.getProperty(key);
            if (StringUtils.hasText(raw) && isFalsyFlag(raw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean envFlagTrueFromRawEnv(String name) {
        try {
            String raw = System.getenv(name);
            return StringUtils.hasText(raw) && isTruthyFlag(raw);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    /**
     * Applies explicit enable keys if present (last matching key wins for typical single-line env files).
     */
    static boolean evaluateParsedEnvMapForEnable(Map<String, Object> map) {
        Optional<Boolean> ex = resolvedExplicitEnableFromParsedMap(map);
        if (ex.isPresent()) {
            return ex.get();
        }
        return implicitWanFromParsedMap(map);
    }

    static Optional<Boolean> resolvedExplicitEnableFromParsedMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }
        Boolean latest = null;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = DotEnvLocalEnvironmentPostProcessor.normalizeEnvKey(e.getKey());
            if (!StringUtils.hasText(key) || !isEnableToggleKey(key)) {
                continue;
            }
            if (isTruthyFlag(e.getValue())) {
                latest = true;
            } else if (isFalsyFlag(e.getValue())) {
                latest = false;
            }
        }
        return latest == null ? Optional.empty() : Optional.of(latest);
    }

    private static boolean implicitWanFromParsedMap(Map<String, Object> map) {
        String provider = mapGetFlexible(map, "IMAGE_NOTES_PROVIDER", "image-notes.provider");
        if (!"wanx".equalsIgnoreCase(provider)) {
            return false;
        }
        String key = firstNonBlankText(
                mapGetFlexible(map, "IMAGE_NOTES_API_KEY", "image-notes.api-key"),
                mapGetFlexible(map, "VISION_API_KEY")
        );
        if (!StringUtils.hasText(key)) {
            return false;
        }
        return true;
    }

    private static String mapGetFlexible(Map<String, Object> map, String... keyAliases) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String nk = DotEnvLocalEnvironmentPostProcessor.normalizeEnvKey(e.getKey());
            for (String want : keyAliases) {
                if (want.equalsIgnoreCase(nk) && e.getValue() != null) {
                    String v = e.getValue().toString().trim();
                    if (StringUtils.hasText(v)) {
                        return v;
                    }
                }
            }
        }
        return "";
    }

    private static boolean isEnableToggleKey(String key) {
        return "IMAGE_NOTES_ENABLED".equalsIgnoreCase(key)
                || "IMAGE_NOTE_ENABLED".equalsIgnoreCase(key)
                || "image-notes.enabled".equalsIgnoreCase(key);
    }

    private static boolean isTruthyFlag(Object raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.toString().trim();
        if (!StringUtils.hasText(s)) {
            return false;
        }
        return "true".equalsIgnoreCase(s)
                || "1".equals(s)
                || "yes".equalsIgnoreCase(s)
                || "on".equalsIgnoreCase(s);
    }

    private static boolean isFalsyFlag(Object raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.toString().trim();
        if (!StringUtils.hasText(s)) {
            return false;
        }
        return "false".equalsIgnoreCase(s)
                || "0".equals(s)
                || "no".equalsIgnoreCase(s)
                || "off".equalsIgnoreCase(s);
    }

    private static String firstNonBlankText(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String c : candidates) {
            if (StringUtils.hasText(c)) {
                return c.trim();
            }
        }
        return "";
    }
}
