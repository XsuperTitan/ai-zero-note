package com.aizeronote.service;

import com.aizeronote.config.AppStorageProperties;
import com.aizeronote.config.VideoCaptureProperties;
import com.aizeronote.model.VideoFrameItem;
import com.aizeronote.model.VideoFrameResult;
import com.aizeronote.model.VideoMetaResult;
import com.aizeronote.model.VideoTextResult;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class VideoLinkParseService {

    private static final String DEFAULT_YT_DLP = "yt-dlp";
    private static final String DEFAULT_FFMPEG = "ffmpeg";
    private static final List<String> YT_DLP_FALLBACK_PATHS = List.of(
            "/opt/homebrew/bin/yt-dlp",
            "/usr/local/bin/yt-dlp",
            "/usr/bin/yt-dlp"
    );
    private static final List<String> FFMPEG_FALLBACK_PATHS = List.of(
            "/opt/homebrew/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/usr/bin/ffmpeg"
    );
    private static final String DEFAULT_FRAMES_SUBDIR = "video-frames";
    private static final double DEFAULT_SCENE_THRESHOLD = 0.4;
    private static final int DEFAULT_INTERVAL_SECONDS = 10;
    private static final int DEFAULT_PROCESS_TIMEOUT_SECONDS = 300;
    private static final double DEFAULT_MIN_SCENE_THRESHOLD = 0.1;
    private static final double DEFAULT_MAX_SCENE_THRESHOLD = 0.9;
    private static final int DEFAULT_CLEANUP_RETENTION_DAYS = 7;
    private static final int COMMAND_OUTPUT_LIMIT = 24_000;
    private static final int DEFAULT_SUBTITLE_MAX_CHARS = 12_000;
    private static final int DEFAULT_TRANSCRIPTION_MAX_CHARS = 16_000;

    private static final Pattern PTS_FILE_PATTERN = Pattern.compile(".*_(\\d+)\\.jpg$");
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^[a-f0-9]{8}$");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final VideoCaptureProperties properties;
    private final Path frameBaseDir;
    private final TranscriptionService transcriptionService;

    public VideoLinkParseService(
            AppStorageProperties appStorageProperties,
            VideoCaptureProperties properties,
            TranscriptionService transcriptionService
    ) {
        this.properties = properties;
        this.transcriptionService = transcriptionService;
        Path outputDir = Path.of(appStorageProperties.outputDir()).toAbsolutePath();
        String framesSubdir = firstText(properties.framesSubdir(), DEFAULT_FRAMES_SUBDIR);
        this.frameBaseDir = outputDir.resolve(framesSubdir).normalize();
        try {
            Files.createDirectories(this.frameBaseDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create video frame directory: " + this.frameBaseDir, ex);
        }
    }

    public VideoMetaResult getVideoMeta(String url) {
        String normalizedUrl = validateUrl(url);
        CommandResult result = executeCommand(ytDlpCommand(
                "--no-playlist",
                "--print", "%(title)s",
                "--print", "%(duration)s",
                "--print", "%(uploader)s",
                "--print", "%(thumbnail)s",
                normalizedUrl
        ), processTimeoutSeconds(), "视频元信息解析失败");

        List<String> lines = result.stdout().lines().map(String::trim).toList();
        String title = valueAt(lines, 0, "Untitled Video");
        long durationSeconds = parseDurationSeconds(valueAt(lines, 1, "0"));
        String uploader = valueAt(lines, 2, "");
        String thumbnail = valueAt(lines, 3, "");

        return new VideoMetaResult(
                normalizedUrl,
                title,
                durationSeconds,
                formatDuration(durationSeconds),
                uploader,
                thumbnail
        );
    }

    public VideoFrameResult extractKeyFrames(String url) {
        String normalizedUrl = validateUrl(url);
        VideoMetaResult meta = getVideoMeta(normalizedUrl);
        cleanExpiredFrames();

        double sceneThreshold = defaultSceneThreshold();
        int intervalSec = defaultIntervalSec();

        String streamUrl = resolveStreamUrl(normalizedUrl);
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path taskDir = frameBaseDir.resolve(taskId).normalize();
        try {
            Files.createDirectories(taskDir);
        } catch (IOException ex) {
            throw new IllegalStateException("创建截图目录失败: " + taskDir, ex);
        }

        String sceneFilter = "select='gt(scene\\," + sceneThreshold + ")',scale=720:-2";
        try {
            runFrameExtraction(streamUrl, taskDir, sceneFilter, intervalSec);
        } catch (IllegalStateException extractionError) {
            if (!shouldFallbackToLocalDownload(extractionError)) {
                throw extractionError;
            }
            Path downloadedVideo = downloadVideoForFallback(normalizedUrl, taskDir);
            runFrameExtraction(downloadedVideo.toString(), taskDir, sceneFilter, intervalSec);
        }

        List<Path> allFrames = listJpgFrames(taskDir);
        if (allFrames.isEmpty()) {
            throw new IllegalStateException("未提取到可用截图，请尝试降低场景阈值后重试。");
        }

        int targetFrameCount = resolveTargetFrameCount(meta.durationSeconds());
        List<Path> selectedFrames = selectPreferredFrames(allFrames, targetFrameCount);
        List<VideoFrameItem> frameItems = selectedFrames.stream()
                .map(path -> new VideoFrameItem(
                        path.getFileName().toString(),
                        "/api/video/download/" + taskId + "/" + path.getFileName(),
                        parsePts(path.getFileName().toString())
                ))
                .toList();

        return new VideoFrameResult(taskId, meta, frameItems);
    }

    public VideoTextResult buildVideoText(String url) {
        String normalizedUrl = validateUrl(url);
        VideoMetaResult meta = getVideoMeta(normalizedUrl);
        String subtitleText = extractSubtitleText(normalizedUrl);
        String textSource = "字幕文本";
        String usableText = subtitleText;
        if (!StringUtils.hasText(usableText)) {
            usableText = extractAudioTranscriptionText(normalizedUrl);
            if (StringUtils.hasText(usableText)) {
                textSource = "音轨转写文本";
            }
        }
        String textContent = buildVideoTextContent(meta, usableText, textSource);
        return new VideoTextResult(meta, subtitleText, textContent);
    }

    public Path resolveFramePath(String taskId, String fileName) {
        if (!StringUtils.hasText(taskId) || !TASK_ID_PATTERN.matcher(taskId).matches()) {
            throw new IllegalArgumentException("Invalid task id.");
        }
        if (!StringUtils.hasText(fileName) || !FILE_NAME_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Invalid screenshot file name.");
        }

        Path taskDir = frameBaseDir.resolve(taskId).normalize();
        if (!taskDir.startsWith(frameBaseDir)) {
            throw new IllegalArgumentException("Invalid screenshot path.");
        }
        Path resolved = taskDir.resolve(fileName).normalize();
        if (!resolved.startsWith(taskDir)) {
            throw new IllegalArgumentException("Invalid screenshot path.");
        }
        return resolved;
    }

    private List<Path> listJpgFrames(Path taskDir) {
        try (var stream = Files.list(taskDir)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jpg"))
                    .toList();
            Map<Long, Path> deduplicated = new LinkedHashMap<>();
            files.stream()
                    .sorted(Comparator.comparingLong(path -> parsePts(path.getFileName().toString())))
                    .forEach(path -> deduplicated.putIfAbsent(parsePts(path.getFileName().toString()), path));
            return new ArrayList<>(deduplicated.values());
        } catch (IOException ex) {
            throw new IllegalStateException("读取截图目录失败: " + taskDir, ex);
        }
    }

    private List<Path> selectPreferredFrames(List<Path> allFrames, int targetCount) {
        if (targetCount <= 0 || allFrames.size() <= targetCount) {
            return allFrames;
        }
        List<Path> intervalFrames = allFrames.stream()
                .filter(path -> path.getFileName().toString().startsWith("interval_"))
                .toList();
        List<Path> sceneFrames = allFrames.stream()
                .filter(path -> path.getFileName().toString().startsWith("scene_"))
                .toList();

        if (!sceneFrames.isEmpty() && !intervalFrames.isEmpty()) {
            int sceneBudget = Math.max(2, targetCount / 3);
            List<Path> sceneSelected = sampleEvenly(sceneFrames, Math.min(sceneBudget, sceneFrames.size()));
            int remainingBudget = Math.max(0, targetCount - sceneSelected.size());
            List<Path> intervalSelected = sampleEvenly(intervalFrames, Math.min(remainingBudget, intervalFrames.size()));

            List<Path> merged = new ArrayList<>();
            merged.addAll(sceneSelected);
            merged.addAll(intervalSelected);
            merged.sort(Comparator.comparingLong(path -> parsePts(path.getFileName().toString())));
            return merged;
        }

        if (!intervalFrames.isEmpty()) {
            return sampleEvenly(intervalFrames, targetCount);
        }

        if (!sceneFrames.isEmpty()) {
            return sampleEvenly(sceneFrames, targetCount);
        }
        return allFrames;
    }

    private List<Path> sampleEvenly(List<Path> source, int targetCount) {
        if (targetCount <= 0 || source.isEmpty() || source.size() <= targetCount) {
            return source;
        }
        if (targetCount == 1) {
            return List.of(source.get(source.size() / 2));
        }
        List<Path> selected = new ArrayList<>();
        double step = (double) (source.size() - 1) / (targetCount - 1);
        for (int i = 0; i < targetCount; i++) {
            int index = (int) Math.round(i * step);
            selected.add(source.get(index));
        }
        return selected;
    }

    private int resolveTargetFrameCount(long durationSeconds) {
        int hardMax = positiveOrDefault(properties.maxFrameCount(), 24);
        int fallbackDefault = positiveOrDefault(properties.defaultMaxCount(), 12);
        if (durationSeconds <= 0) {
            int fallbackTarget = clamp(fallbackDefault, 12, 16);
            return Math.min(hardMax, fallbackTarget);
        }
        int target;
        if (durationSeconds > 20 * 60) {
            int byDuration = (int) Math.ceil((double) durationSeconds / 75.0);
            target = clamp(byDuration, 16, 24);
        } else {
            int byDuration = (int) Math.ceil((double) durationSeconds / 90.0);
            target = clamp(byDuration, 12, 16);
        }
        return Math.min(hardMax, target);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String resolveStreamUrl(String url) {
        CommandResult result = executeCommand(ytDlpCommand(
                "-g",
                "--no-playlist",
                url
        ), processTimeoutSeconds(), "视频流地址解析失败");

        return result.stdout().lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("视频流地址解析失败：未返回可用地址。"));
    }

    private void runFrameExtraction(String input, Path taskDir, String sceneFilter, int intervalSec) {
        executeCommand(List.of(
                ffmpegPath(),
                "-y",
                "-i", input,
                "-vf", sceneFilter,
                "-vsync", "vfr",
                "-frame_pts", "1",
                taskDir.resolve("scene_%010d.jpg").toString()
        ), processTimeoutSeconds(), "关键帧提取失败（场景检测）");

        executeCommand(List.of(
                ffmpegPath(),
                "-y",
                "-i", input,
                "-vf", "fps=1/" + intervalSec + ",scale=720:-2",
                "-frame_pts", "1",
                taskDir.resolve("interval_%010d.jpg").toString()
        ), processTimeoutSeconds(), "关键帧提取失败（等间隔补充）");
    }

    private boolean shouldFallbackToLocalDownload(IllegalStateException error) {
        String message = error.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("403")
                || lower.contains("forbidden")
                || lower.contains("access denied");
    }

    private Path downloadVideoForFallback(String url, Path taskDir) {
        executeCommand(ytDlpCommand(
                "--no-playlist",
                "-f", "mp4/best",
                "-o", taskDir.resolve("source.%(ext)s").toString(),
                url
        ), processTimeoutSeconds(), "关键帧提取失败（下载视频回退）");

        try (Stream<Path> files = Files.list(taskDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("source."))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("关键帧提取失败（下载视频回退）：未找到下载结果。"));
        } catch (IOException ioException) {
            throw new IllegalStateException("关键帧提取失败（下载视频回退）：读取下载目录失败。", ioException);
        }
    }

    private String extractSubtitleText(String url) {
        Path subtitleDir = frameBaseDir.resolve("subtitle-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        try {
            Files.createDirectories(subtitleDir);
            executeCommand(ytDlpCommand(
                    "--no-playlist",
                    "--skip-download",
                    "--write-auto-sub",
                    "--write-sub",
                    "--sub-langs", "zh-Hans,zh-CN,zh,en",
                    "--sub-format", "vtt",
                    "-o", subtitleDir.resolve("subtitle.%(ext)s").toString(),
                    url
            ), processTimeoutSeconds(), "视频字幕提取失败");

            try (Stream<Path> files = Files.list(subtitleDir)) {
                Path subtitleFile = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".vtt"))
                        .findFirst()
                        .orElse(null);
                if (subtitleFile == null) {
                    return "";
                }
                String plainText = parseVttToText(subtitleFile);
                return trimToLimit(plainText, DEFAULT_SUBTITLE_MAX_CHARS);
            }
        } catch (Exception ignored) {
            return "";
        } finally {
            deleteDirectoryQuietly(subtitleDir);
        }
    }

    private String parseVttToText(Path subtitleFile) throws IOException {
        List<String> lines = Files.readAllLines(subtitleFile, StandardCharsets.UTF_8);
        List<String> contents = new ArrayList<>();
        String previous = "";
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if ("WEBVTT".equalsIgnoreCase(line)
                    || line.startsWith("NOTE")
                    || line.startsWith("Kind:")
                    || line.startsWith("Language:")
                    || line.matches("^\\d+$")
                    || line.contains("-->")) {
                continue;
            }
            String normalized = line.replaceAll("<[^>]+>", "").trim();
            if (!StringUtils.hasText(normalized) || normalized.equals(previous)) {
                continue;
            }
            contents.add(normalized);
            previous = normalized;
        }
        return String.join("\n", contents);
    }

    private String extractAudioTranscriptionText(String url) {
        Path audioDir = frameBaseDir.resolve("audio-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        try {
            Files.createDirectories(audioDir);
            executeCommand(ytDlpCommand(
                    "--no-playlist",
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "-o", audioDir.resolve("audio.%(ext)s").toString(),
                    url
            ), processTimeoutSeconds(), "视频音轨提取失败");

            Path audioFile;
            try (Stream<Path> files = Files.list(audioDir)) {
                audioFile = files
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                            return name.endsWith(".mp3")
                                    || name.endsWith(".m4a")
                                    || name.endsWith(".webm")
                                    || name.endsWith(".wav");
                        })
                        .findFirst()
                        .orElse(null);
            }
            if (audioFile == null) {
                return "";
            }
            byte[] bytes = Files.readAllBytes(audioFile);
            if (bytes.length == 0) {
                return "";
            }
            Path audioFilename = audioFile.getFileName();
            if (audioFilename == null) {
                return "";
            }
            MultipartFile multipartFile = new InMemoryMultipartFile(audioFilename.toString(), bytes);
            String transcription = transcriptionService.transcribeWithDefaultProvider(multipartFile);
            return trimToLimit(transcription, DEFAULT_TRANSCRIPTION_MAX_CHARS);
        } catch (Exception ignored) {
            return "";
        } finally {
            deleteDirectoryQuietly(audioDir);
        }
    }

    private String buildVideoTextContent(VideoMetaResult meta, String mainText, String textSourceLabel) {
        StringBuilder builder = new StringBuilder();
        builder.append("以下是视频学习素材，请基于这些内容生成结构化学习笔记。\n\n")
                .append("【视频信息】\n")
                .append("标题：").append(meta.title()).append("\n")
                .append("链接：").append(meta.sourceUrl()).append("\n")
                .append("时长：").append(meta.durationText()).append("\n");
        if (StringUtils.hasText(meta.uploader())) {
            builder.append("作者：").append(meta.uploader()).append("\n");
        }
        if (StringUtils.hasText(mainText)) {
            builder.append("\n【").append(textSourceLabel).append("】\n")
                    .append(mainText)
                    .append("\n");
        } else {
            builder.append("\n【视频文本】\n")
                    .append("未获取到可用字幕或音轨转写结果，请结合视频链接与标题理解内容。\n");
        }
        return builder.toString().trim();
    }

    private String validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("视频链接不能为空。");
        }
        try {
            java.net.URI uri = java.net.URI.create(url.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("视频链接必须以 http 或 https 开头。");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("视频链接无效，请检查后重试。");
            }
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("视频链接无效，请检查后重试。", ex);
        }
    }

    private double defaultSceneThreshold() {
        double configuredDefault = properties.defaultSceneThreshold() == null
                ? DEFAULT_SCENE_THRESHOLD
                : properties.defaultSceneThreshold();
        double min = properties.minSceneThreshold() == null
                ? DEFAULT_MIN_SCENE_THRESHOLD
                : properties.minSceneThreshold();
        double max = properties.maxSceneThreshold() == null
                ? DEFAULT_MAX_SCENE_THRESHOLD
                : properties.maxSceneThreshold();
        double value = configuredDefault;
        if (value < min || value > max) {
            throw new IllegalStateException("默认场景阈值配置非法，需在 " + min + " 到 " + max + " 之间。");
        }
        return value;
    }

    private int defaultIntervalSec() {
        return positiveOrDefault(properties.defaultIntervalSec(), DEFAULT_INTERVAL_SECONDS);
    }

    private CommandResult executeCommand(List<String> command, int timeoutSeconds, String errorPrefix) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(frameBaseDir.toFile());
        try {
            Process process = processBuilder.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread stdoutReader = createReaderThread(process.getInputStream(), stdout);
            Thread stderrReader = createReaderThread(process.getErrorStream(), stderr);
            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(errorPrefix + "：处理超时，请稍后重试。");
            }
            stdoutReader.join(2000);
            stderrReader.join(2000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException(errorPrefix + "：" + normalizeCommandErrorMessage(tailMessage(stderr, stdout)));
            }
            return new CommandResult(stdout.toString().trim(), stderr.toString().trim(), exitCode);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(errorPrefix + "：任务被中断。", interruptedException);
        } catch (IOException ioException) {
            String normalizedError = normalizeCommandErrorMessage(ioException.getMessage());
            throw new IllegalStateException(errorPrefix + "：" + normalizedError, ioException);
        }
    }

    private Thread createReaderThread(InputStream inputStream, StringBuilder outputBuilder) {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLimited(outputBuilder, line + "\n");
                }
            } catch (IOException ignored) {
                // Ignore stream close exceptions.
            }
        });
    }

    private void appendLimited(StringBuilder builder, String text) {
        if (builder.length() >= COMMAND_OUTPUT_LIMIT) {
            return;
        }
        int available = COMMAND_OUTPUT_LIMIT - builder.length();
        if (text.length() <= available) {
            builder.append(text);
            return;
        }
        builder.append(text, 0, available);
    }

    private String tailMessage(StringBuilder stderr, StringBuilder stdout) {
        String content = StringUtils.hasText(stderr.toString()) ? stderr.toString() : stdout.toString();
        if (!StringUtils.hasText(content)) {
            return "外部命令执行失败。";
        }
        String trimmed = content.trim();
        int maxLen = 240;
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(trimmed.length() - maxLen);
    }

    private String normalizeCommandErrorMessage(String rawMessage) {
        if (!StringUtils.hasText(rawMessage)) {
            return "外部命令执行失败。";
        }
        String lower = rawMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("cookies")
                || lower.contains("sign in")
                || lower.contains("confirm you're not a bot")
                || lower.contains("confirm you’re not a bot")
                || lower.contains("use --cookies")) {
            return "视频平台需要登录态(cookies)。请在配置中设置 VIDEO_YT_DLP_COOKIES_FROM_BROWSER=chrome 或 VIDEO_YT_DLP_COOKIE_FILE=/path/to/cookies.txt 后重试。";
        }
        return rawMessage;
    }

    private int processTimeoutSeconds() {
        return positiveOrDefault(properties.processTimeoutSeconds(), DEFAULT_PROCESS_TIMEOUT_SECONDS);
    }

    private void cleanExpiredFrames() {
        int retentionDays = positiveOrDefault(properties.cleanupRetentionDays(), DEFAULT_CLEANUP_RETENTION_DAYS);
        long now = System.currentTimeMillis();
        long threshold = now - Duration.ofDays(retentionDays).toMillis();
        try (Stream<Path> stream = Files.list(frameBaseDir)) {
            stream.filter(Files::isDirectory)
                    .forEach(path -> deleteDirectoryIfExpired(path, threshold));
        } catch (IOException ex) {
            // Cleanup failures should not interrupt core request flow.
        }
    }

    private void deleteDirectoryIfExpired(Path dir, long threshold) {
        try {
            long modified = Files.getLastModifiedTime(dir).toMillis();
            if (modified >= threshold) {
                return;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Ignore deletion failures to avoid blocking request lifecycle.
                    }
                });
            }
        } catch (IOException ignored) {
            // Ignore cleanup failures to keep request path stable.
        }
    }

    private String ytDlpPath() {
        return resolveExecutable(
                firstText(properties.ytDlpPath(), DEFAULT_YT_DLP),
                YT_DLP_FALLBACK_PATHS,
                "yt-dlp",
                "VIDEO_YT_DLP_PATH"
        );
    }

    private List<String> ytDlpCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath());

        String cookieFile = properties.ytDlpCookieFile();
        String cookiesFromBrowser = properties.ytDlpCookiesFromBrowser();
        if (StringUtils.hasText(cookieFile)) {
            command.add("--cookies");
            command.add(cookieFile.trim());
        } else if (StringUtils.hasText(cookiesFromBrowser)) {
            command.add("--cookies-from-browser");
            command.add(cookiesFromBrowser.trim());
        }

        command.addAll(List.of(args));
        return command;
    }

    private String ffmpegPath() {
        return resolveExecutable(
                firstText(properties.ffmpegPath(), DEFAULT_FFMPEG),
                FFMPEG_FALLBACK_PATHS,
                "ffmpeg",
                "VIDEO_FFMPEG_PATH"
        );
    }

    private String valueAt(List<String> values, int index, String fallback) {
        if (index >= values.size()) {
            return fallback;
        }
        String value = values.get(index);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private long parsePts(String filename) {
        Matcher matcher = PTS_FILE_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return Long.MAX_VALUE;
        }
        return parseLong(matcher.group(1), Long.MAX_VALUE);
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long parseDurationSeconds(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Math.max(0L, Math.round(Double.parseDouble(value.trim())));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String formatDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            return "00:00";
        }
        Duration duration = Duration.ofSeconds(durationSeconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (hours > 0) {
            return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        }
        return "%02d:%02d".formatted(duration.toMinutes(), seconds);
    }

    private int positiveOrDefault(Integer value, int fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String resolveExecutable(
            String configuredValue,
            List<String> fallbackPaths,
            String toolName,
            String envName
    ) {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(configuredValue)) {
            candidates.add(configuredValue.trim());
        }
        candidates.addAll(fallbackPaths);

        for (String candidate : candidates) {
            String resolved = resolveCandidate(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new IllegalStateException(
                "未找到 " + toolName + " 可执行文件。请先安装并配置环境变量 " + envName
                        + "（例如 /opt/homebrew/bin/" + toolName + "）。"
        );
    }

    private String resolveCandidate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String value = candidate.trim();
        if (value.contains("/") || value.contains("\\")) {
            Path path = Path.of(value);
            if (Files.exists(path) && Files.isRegularFile(path) && Files.isExecutable(path)) {
                return path.toAbsolutePath().toString();
            }
            return null;
        }
        String onPath = searchExecutableOnPath(value);
        return onPath == null ? null : onPath;
    }

    private String searchExecutableOnPath(String executableName) {
        String pathEnv = System.getenv("PATH");
        if (!StringUtils.hasText(pathEnv)) {
            return null;
        }
        String[] dirs = pathEnv.split(Pattern.quote(File.pathSeparator));
        for (String dir : dirs) {
            if (!StringUtils.hasText(dir)) {
                continue;
            }
            Path candidate = Path.of(dir, executableName);
            if (Files.exists(candidate) && Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return null;
    }

    private void deleteDirectoryQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Ignore best-effort cleanup failures.
                }
            });
        } catch (IOException ignored) {
            // Ignore cleanup failures.
        }
    }

    private String trimToLimit(String value, int maxChars) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= maxChars ? value : value.substring(0, maxChars);
    }

    private record CommandResult(String stdout, String stderr, int exitCode) {
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final byte[] content;

        private InMemoryMultipartFile(String originalFilename, byte[] content) {
            this.originalFilename = originalFilename;
            this.content = content;
        }

        @Override
        @NonNull
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "audio/mpeg";
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        @NonNull
        public byte[] getBytes() {
            return Objects.requireNonNull(content);
        }

        @Override
        @NonNull
        public InputStream getInputStream() {
            return new ByteArrayInputStream(Objects.requireNonNull(content));
        }

        @Override
        public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}
