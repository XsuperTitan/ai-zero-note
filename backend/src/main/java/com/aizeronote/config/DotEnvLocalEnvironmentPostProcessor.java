package com.aizeronote.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loads {@code .env.local} even when JVM {@code user.dir} is not repo root —
 * scans {@code user.dir} plus parent directories for {@code .env.local} / {@code backend/.env.local}.
 * Placed immediately after OS environment variables so exported {@code IMAGE_NOTES_*} still wins.
 */
public class DotEnvLocalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLocalEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "aiZeroNoteDotEnvLocal";

    /**
     * Run early so {@code IMAGE_NOTES_*} entries exist before other {@link EnvironmentPostProcessor}s and
     * {@code spring.config.import} interpolate {@link org.springframework.boot.context.properties.ConfigurationProperties}
     * placeholders in {@code application.yml}.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path found = locateFirstReadableEnvLocal();
        if (found == null) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "DotEnvLocalEnvironmentPostProcessor: no readable .env.local near user.dir={}, "
                                + "(checked parent dirs for .env.local and backend/.env.local)",
                        Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize());
            }
            return;
        }
        Map<String, Object> map;
        try {
            map = parse(readUtf8Lines(found));
        } catch (IOException ex) {
            log.warn("Cannot read {}: {}", found.toAbsolutePath(), ex.getMessage());
            return;
        }
        if (map.isEmpty()) {
            return;
        }
        addConfigurationPropertyAliases(map);

        if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources()
                    .addAfter(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            new MapPropertySource(PROPERTY_SOURCE_NAME, map)
                    );
        } else {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
        }
        log.info(
                "Loaded {} keys from {} — image-notes.enabled={}",
                map.size(),
                found.toAbsolutePath(),
                environment.getProperty("image-notes.enabled", "(unset)")
        );
    }

    /**
     * First readable {@code .env.local} adjacent to JVM {@link System#getProperty(String) user.dir},
     * or under a {@code backend/} subdirectory while walking ancestors.
     */
    public static Path locateFirstReadableEnvLocal() {
        Path start = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (Path p : candidatePaths(start)) {
            if (Files.isRegularFile(p) && Files.isReadable(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    static List<Path> candidatePaths(Path start) {
        List<Path> ordered = new ArrayList<>();
        Objects.requireNonNull(start, "start");
        Path cur = start;
        for (int depth = 0; depth <= 12 && cur != null; depth++) {
            ordered.add(cur.resolve(".env.local"));
            ordered.add(cur.resolve("backend").resolve(".env.local"));
            cur = cur.getParent();
        }
        return ordered;
    }

    /** Trims BOM from key tokens (Excel / editors sometimes prepend U+FEFF at line start only; rare on key itself). */
    public static String normalizeEnvKey(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            return "";
        }
        String key = rawKey.trim();
        if (!key.isEmpty() && key.charAt(0) == '\ufeff') {
            key = key.substring(1).trim();
        }
        return key;
    }

    private static List<String> readUtf8Lines(Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        int offset = 0;
        if (raw.length >= 3 && raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF) {
            offset = 3;
        }
        String text = new String(raw, offset, raw.length - offset, StandardCharsets.UTF_8);
        return text.lines().collect(Collectors.toList());
    }

    /**
     * {@link MapPropertySource} keys are matched literally — unlike {@code SYSTEM_ENVIRONMENT},
     * {@code IMAGE_NOTES_ENABLED} alone may not populate {@link ImageNotesProperties#enabled}.
     * Mirror ENV-style keys used in {@code backend/.env.local} into canonical {@code image-notes.*}.
     */
    static void addConfigurationPropertyAliases(Map<String, Object> map) {
        copyEnvToCanonical(map, "IMAGE_NOTES_ENABLED", "image-notes.enabled");
        copyEnvToCanonical(map, "IMAGE_NOTES_PROVIDER", "image-notes.provider");
        copyEnvToCanonical(map, "IMAGE_NOTES_BASE_URL", "image-notes.base-url");
        copyEnvToCanonical(map, "IMAGE_NOTES_WAN_BASE_URL", "image-notes.wan-base-url");
        copyEnvToCanonical(map, "IMAGE_NOTES_API_KEY", "image-notes.api-key");
        copyEnvToCanonical(map, "IMAGE_NOTES_MODEL", "image-notes.model");
        copyEnvToCanonical(map, "IMAGE_NOTES_SIZE", "image-notes.size");
        copyEnvToCanonical(map, "IMAGE_NOTES_MAX_SOURCE_CHARS", "image-notes.max-source-chars");
    }

    private static void copyEnvToCanonical(Map<String, Object> map, String envKey, String canonicalKey) {
        if (!map.containsKey(envKey)) {
            return;
        }
        Object raw = map.get(envKey);
        if (raw == null) {
            return;
        }
        String trimmed = raw.toString().trim();
        if (!StringUtils.hasText(trimmed)) {
            return;
        }
        if (!map.containsKey(canonicalKey)) {
            map.put(canonicalKey, trimmed);
        }
    }

    public static Map<String, Object> loadParsedEnvLocal(Path path) throws IOException {
        Map<String, Object> map = parse(readUtf8Lines(path));
        addConfigurationPropertyAliases(map);
        return map;
    }

    static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = normalizeEnvKey(trimmed.substring(0, eq));
            String value = trimmed.substring(eq + 1).trim();
            value = stripTrailingInlineCommentBeforeQuote(value);
            if (!key.isEmpty()) {
                out.put(key, unquoteMaybe(value));
            }
        }
        return out;
    }

    /**
     * Strips trailing {@code # comment} unless the value starts as a quoted token (quotes handled in {@link #unquoteMaybe}).
     */
    static String stripTrailingInlineCommentBeforeQuote(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '"' || first == '\'') {
            return value;
        }
        int hash = value.indexOf('#');
        if (hash < 0) {
            return value;
        }
        return value.substring(0, hash).trim();
    }

    private static String unquoteMaybe(String value) {
        if (value.length() >= 2) {
            char a = value.charAt(0);
            char z = value.charAt(value.length() - 1);
            if ((a == '"' && z == '"') || (a == '\'' && z == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
