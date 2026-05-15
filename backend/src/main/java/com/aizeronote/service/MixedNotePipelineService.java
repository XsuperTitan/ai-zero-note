package com.aizeronote.service;

import com.aizeronote.config.AppStorageProperties;
import com.aizeronote.model.NoteResult;
import com.aizeronote.model.NoteStyle;
import com.aizeronote.model.NoteSummary;
import com.aizeronote.model.OutputLanguage;
import com.aizeronote.service.guidance.GuidanceCheckInService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MixedNotePipelineService {

    private static final long MB = 1024L * 1024L;
    private static final Pattern VIDEO_TITLE_LINE_PATTERN = Pattern.compile("^标题\\s*[：:]\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern VIDEO_MARKDOWN_TITLE_PATTERN = Pattern.compile("\\*\\*视频标题\\*\\*\\s*:\\s*\\[(.+?)]\\(");

    private final AppStorageProperties appStorageProperties;
    private final TranscriptionService transcriptionService;
    private final SummaryService summaryService;
    private final MarkdownService markdownService;
    private final GuidanceCheckInService guidanceCheckInService;

    public MixedNotePipelineService(
            AppStorageProperties appStorageProperties,
            TranscriptionService transcriptionService,
            SummaryService summaryService,
            MarkdownService markdownService,
            GuidanceCheckInService guidanceCheckInService
    ) {
        this.appStorageProperties = appStorageProperties;
        this.transcriptionService = transcriptionService;
        this.summaryService = summaryService;
        this.markdownService = markdownService;
        this.guidanceCheckInService = guidanceCheckInService;
    }

    public NoteResult processMixed(
            Long userId,
            MultipartFile audioFile,
            MultipartFile textFile,
            String textContent,
            NoteStyle noteStyle,
            OutputLanguage outputLanguage,
            Long guidanceCheckInId
    ) {
        boolean hasAudio = audioFile != null && !audioFile.isEmpty();
        boolean hasTextFile = textFile != null && !textFile.isEmpty();
        boolean hasTextContent = StringUtils.hasText(textContent);
        String checkInBlock = guidanceCheckInService.supplementalBlockForNotePipeline(userId, guidanceCheckInId);
        if (!hasAudio && !hasTextFile && !hasTextContent && !StringUtils.hasText(checkInBlock)) {
            throw new IllegalArgumentException("Please provide audio file, text file, text content, or a guidance check-in with material.");
        }

        if (hasAudio) {
            validateAudioFile(audioFile);
        }
        if (hasTextFile) {
            validateTextFile(textFile);
        }

        String transcription = hasAudio ? transcriptionService.transcribeWithDefaultProvider(audioFile) : "";
        String userSupplemental = mergeSupplementalText(textFile, textContent);
        String supplementalText = mergeCheckInWithUserSupplement(checkInBlock, userSupplemental);

        NoteSummary summary = summaryService.summarizeWithSupplemental(
                transcription,
                supplementalText,
                noteStyle,
                outputLanguage
        );
        String sourceName = buildSourceName(audioFile, textFile, textContent, summary.title());
        MarkdownService.GeneratedMarkdown generated = markdownService.generate(sourceName, transcription, summary);

        String displayedTranscription = StringUtils.hasText(transcription) ? transcription : supplementalText;
        String mindPayload = summary.mindMapJson() != null ? summary.mindMapJson() : "";
        mindPayload = noteStyle != NoteStyle.MIND_MAP ? "" : mindPayload;
        return new NoteResult(
                generated.noteId(),
                sourceName,
                displayedTranscription,
                summary.title(),
                summary.abstractText(),
                summary.keyPoints(),
                summary.codeSnippets(),
                summary.todos(),
                generated.markdownPreview(),
                "/api/notes/download/" + generated.filePath().getFileName(),
                noteStyle,
                outputLanguage,
                mindPayload
        );
    }

    private String buildSourceName(
            MultipartFile audioFile,
            MultipartFile textFile,
            String textContent,
            String summaryTitle
    ) {
        boolean hasAudio = audioFile != null && !audioFile.isEmpty();
        boolean hasTextFile = textFile != null && !textFile.isEmpty();
        boolean hasTextContent = StringUtils.hasText(textContent);
        String audioName = "";
        if (hasAudio) {
            audioName = Objects.requireNonNull(audioFile).getOriginalFilename();
        }
        String textName = "";
        if (hasTextFile) {
            textName = Objects.requireNonNull(textFile).getOriginalFilename();
        }
        if (hasAudio && (hasTextFile || hasTextContent)) {
            return audioName + " + text";
        }
        if (hasAudio) {
            return audioName;
        }
        if (hasTextFile) {
            return textName;
        }
        if (StringUtils.hasText(summaryTitle)) {
            return summaryTitle.trim();
        }
        String extractedTitle = extractVideoTitleFromText(textContent);
        if (StringUtils.hasText(extractedTitle)) {
            return extractedTitle;
        }
        return "text-input";
    }

    private String extractVideoTitleFromText(String textContent) {
        if (!StringUtils.hasText(textContent)) {
            return "";
        }
        Matcher lineMatcher = VIDEO_TITLE_LINE_PATTERN.matcher(textContent);
        if (lineMatcher.find()) {
            return lineMatcher.group(1).trim();
        }
        Matcher markdownMatcher = VIDEO_MARKDOWN_TITLE_PATTERN.matcher(textContent);
        if (markdownMatcher.find()) {
            return markdownMatcher.group(1).trim();
        }
        return "";
    }

    private String mergeSupplementalText(MultipartFile textFile, String textContent) {
        StringBuilder builder = new StringBuilder();
        if (textFile != null && !textFile.isEmpty()) {
            try {
                builder.append(new String(textFile.getBytes(), StandardCharsets.UTF_8).trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Failed to read uploaded text file.");
            }
        }
        if (StringUtils.hasText(textContent)) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(textContent.trim());
        }
        return builder.toString();
    }

    private static String mergeCheckInWithUserSupplement(String checkInBlock, String userSupplemental) {
        if (!StringUtils.hasText(checkInBlock)) {
            return userSupplemental == null ? "" : userSupplemental;
        }
        if (!StringUtils.hasText(userSupplemental)) {
            return checkInBlock;
        }
        return checkInBlock + "\n\n---\n\n" + userSupplemental;
    }

    private void validateAudioFile(MultipartFile file) {
        if (file.getSize() > appStorageProperties.maxUploadSizeBytes()) {
            long maxMb = appStorageProperties.maxUploadSizeBytes() / MB;
            throw new IllegalArgumentException("Audio file is too large. Max allowed size is " + maxMb + "MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("Only .mp3 audio files are supported.");
        }
    }

    private void validateTextFile(MultipartFile file) {
        if (file.getSize() > appStorageProperties.maxUploadSizeBytes()) {
            long maxMb = appStorageProperties.maxUploadSizeBytes() / MB;
            throw new IllegalArgumentException("Text file is too large. Max allowed size is " + maxMb + "MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid text file.");
        }
        String lowerName = filename.toLowerCase();
        if (!lowerName.endsWith(".txt") && !lowerName.endsWith(".md")) {
            throw new IllegalArgumentException("Text file must be .txt or .md.");
        }
    }
}
