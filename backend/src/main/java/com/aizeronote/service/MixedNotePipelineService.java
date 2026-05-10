package com.aizeronote.service;

import com.aizeronote.config.AppStorageProperties;
import com.aizeronote.model.NoteResult;
import com.aizeronote.model.NoteSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public class MixedNotePipelineService {

    private static final long MB = 1024L * 1024L;

    private final AppStorageProperties appStorageProperties;
    private final TranscriptionService transcriptionService;
    private final SummaryService summaryService;
    private final MarkdownService markdownService;

    public MixedNotePipelineService(
            AppStorageProperties appStorageProperties,
            TranscriptionService transcriptionService,
            SummaryService summaryService,
            MarkdownService markdownService
    ) {
        this.appStorageProperties = appStorageProperties;
        this.transcriptionService = transcriptionService;
        this.summaryService = summaryService;
        this.markdownService = markdownService;
    }

    public NoteResult processMixed(MultipartFile audioFile, MultipartFile textFile, String textContent) {
        boolean hasAudio = audioFile != null && !audioFile.isEmpty();
        boolean hasTextFile = textFile != null && !textFile.isEmpty();
        boolean hasTextContent = StringUtils.hasText(textContent);
        if (!hasAudio && !hasTextFile && !hasTextContent) {
            throw new IllegalArgumentException("Please provide audio file, text file, or text content.");
        }

        if (hasAudio) {
            validateAudioFile(audioFile);
        }
        if (hasTextFile) {
            validateTextFile(textFile);
        }

        String transcription = hasAudio ? transcriptionService.transcribeWithDefaultProvider(audioFile) : "";
        String supplementalText = mergeSupplementalText(textFile, textContent);

        NoteSummary summary = summaryService.summarizeWithSupplemental(transcription, supplementalText);
        String sourceName = buildSourceName(audioFile, textFile, hasTextContent);
        MarkdownService.GeneratedMarkdown generated = markdownService.generate(sourceName, transcription, summary);

        String displayedTranscription = StringUtils.hasText(transcription) ? transcription : supplementalText;
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
                "/api/notes/download/" + generated.filePath().getFileName()
        );
    }

    private String buildSourceName(MultipartFile audioFile, MultipartFile textFile, boolean hasTextContent) {
        boolean hasAudio = audioFile != null && !audioFile.isEmpty();
        boolean hasTextFile = textFile != null && !textFile.isEmpty();
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
        return "text-input";
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
