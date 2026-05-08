package com.aizeronote.service;

import com.aizeronote.config.AppStorageProperties;
import com.aizeronote.model.NoteResult;
import com.aizeronote.model.NoteSummary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NotePipelineService {

    private static final long MB = 1024L * 1024L;

    private final AppStorageProperties appStorageProperties;
    private final TranscriptionService transcriptionService;
    private final SummaryService summaryService;
    private final MarkdownService markdownService;

    public NotePipelineService(
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

    public NoteResult process(MultipartFile file) {
        validateFile(file);

        String transcription = transcriptionService.transcribeWithDefaultProvider(file);
        NoteSummary summary = summaryService.summarize(transcription);
        MarkdownService.GeneratedMarkdown generated = markdownService.generate(
                file.getOriginalFilename(),
                transcription,
                summary
        );

        return new NoteResult(
                generated.noteId(),
                file.getOriginalFilename(),
                transcription,
                summary.title(),
                summary.abstractText(),
                summary.keyPoints(),
                summary.codeSnippets(),
                summary.todos(),
                generated.markdownPreview(),
                "/api/notes/download/" + generated.filePath().getFileName()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required.");
        }
        if (file.getSize() > appStorageProperties.maxUploadSizeBytes()) {
            long maxMb = appStorageProperties.maxUploadSizeBytes() / MB;
            throw new IllegalArgumentException("File is too large. Max allowed size is " + maxMb + "MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("Only .mp3 files are supported in this MVP.");
        }
    }
}
