package com.aizeronote.controller;

import com.aizeronote.model.NoteResult;
import com.aizeronote.model.TranscriptionComparisonResult;
import com.aizeronote.model.AsrStatus;
import com.aizeronote.service.MarkdownService;
import com.aizeronote.service.MixedNotePipelineService;
import com.aizeronote.service.NotePipelineService;
import com.aizeronote.service.TranscriptionComparisonService;
import com.aizeronote.service.TranscriptionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Objects;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NotePipelineService notePipelineService;
    private final MixedNotePipelineService mixedNotePipelineService;
    private final MarkdownService markdownService;
    private final TranscriptionComparisonService transcriptionComparisonService;
    private final TranscriptionService transcriptionService;

    public NoteController(
            NotePipelineService notePipelineService,
            MixedNotePipelineService mixedNotePipelineService,
            MarkdownService markdownService,
            TranscriptionComparisonService transcriptionComparisonService,
            TranscriptionService transcriptionService
    ) {
        this.notePipelineService = notePipelineService;
        this.mixedNotePipelineService = mixedNotePipelineService;
        this.markdownService = markdownService;
        this.transcriptionComparisonService = transcriptionComparisonService;
        this.transcriptionService = transcriptionService;
    }

    @PostMapping(path = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResult> process(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(notePipelineService.process(file));
    }

    @PostMapping(path = "/process-mixed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResult> processMixed(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "textFile", required = false) MultipartFile textFile,
            @RequestParam(value = "textContent", required = false) String textContent
    ) {
        return ResponseEntity.ok(mixedNotePipelineService.processMixed(file, textFile, textContent));
    }

    @GetMapping("/asr-status")
    public ResponseEntity<AsrStatus> asrStatus() {
        return ResponseEntity.ok(transcriptionService.getAsrStatus());
    }

    @PostMapping(path = "/compare-transcription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionComparisonResult> compareTranscription(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "referenceText", required = false) String referenceText
    ) {
        return ResponseEntity.ok(transcriptionComparisonService.compare(file, referenceText));
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) throws MalformedURLException {
        Path path = markdownService.resolveMarkdownPath(fileName);
        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri()));
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(Objects.requireNonNull(MediaType.TEXT_MARKDOWN))
                .body(resource);
    }
}
