package com.aizeronote.controller;

import com.aizeronote.model.NoteResult;
import com.aizeronote.model.NoteStyle;
import com.aizeronote.model.OutputLanguage;
import com.aizeronote.model.TranscriptionComparisonResult;
import com.aizeronote.model.AsrStatus;
import com.aizeronote.model.dto.ImageNoteEnqueueResponse;
import com.aizeronote.model.dto.ImageNoteStartRequest;
import com.aizeronote.model.dto.ImageNoteStatusResponse;
import com.aizeronote.repository.NoteJobRepository;
import com.aizeronote.service.ImageNoteJobService;
import com.aizeronote.service.ImageNoteStorageService;
import com.aizeronote.service.MarkdownService;
import com.aizeronote.service.MixedNotePipelineService;
import com.aizeronote.service.NoteJobService;
import com.aizeronote.service.NotePipelineService;
import com.aizeronote.service.TranscriptionComparisonService;
import com.aizeronote.service.TranscriptionService;
import com.aizeronote.service.UserService;
import com.aizeronote.service.guidance.GuidanceCheckInService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final UserService userService;
    private final NoteJobService noteJobService;
    private final NoteJobRepository noteJobRepository;
    private final ImageNoteJobService imageNoteJobService;
    private final ImageNoteStorageService imageNoteStorageService;
    private final GuidanceCheckInService guidanceCheckInService;

    public NoteController(
            NotePipelineService notePipelineService,
            MixedNotePipelineService mixedNotePipelineService,
            MarkdownService markdownService,
            TranscriptionComparisonService transcriptionComparisonService,
            TranscriptionService transcriptionService,
            UserService userService,
            NoteJobService noteJobService,
            NoteJobRepository noteJobRepository,
            ImageNoteJobService imageNoteJobService,
            ImageNoteStorageService imageNoteStorageService,
            GuidanceCheckInService guidanceCheckInService
    ) {
        this.notePipelineService = notePipelineService;
        this.mixedNotePipelineService = mixedNotePipelineService;
        this.markdownService = markdownService;
        this.transcriptionComparisonService = transcriptionComparisonService;
        this.transcriptionService = transcriptionService;
        this.userService = userService;
        this.noteJobService = noteJobService;
        this.noteJobRepository = noteJobRepository;
        this.imageNoteJobService = imageNoteJobService;
        this.imageNoteStorageService = imageNoteStorageService;
        this.guidanceCheckInService = guidanceCheckInService;
    }

    @PostMapping(path = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResult> process(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long userId = userService.getLoginUser(request).getId();
        NoteResult result = notePipelineService.process(file);
        noteJobService.tryPersist(userId, result);
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "/process-mixed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResult> processMixed(
            HttpServletRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "textFile", required = false) MultipartFile textFile,
            @RequestParam(value = "textContent", required = false) String textContent,
            @RequestParam(value = "noteStyle", required = false, defaultValue = "LEARNING") String noteStyle,
            @RequestParam(value = "outputLanguage", required = false, defaultValue = "AUTO") String outputLanguage,
            @RequestParam(value = "guidanceCheckInId", required = false) Long guidanceCheckInId
    ) {
        Long userId = userService.getLoginUser(request).getId();
        NoteStyle style = NoteEnumsParser.parseNoteStyle(noteStyle, NoteStyle.LEARNING);
        OutputLanguage lang = NoteEnumsParser.parseOutputLanguage(outputLanguage, OutputLanguage.AUTO);
        NoteResult result = mixedNotePipelineService.processMixed(
                userId, file, textFile, textContent, style, lang, guidanceCheckInId);
        noteJobService.tryPersist(userId, result);
        if (guidanceCheckInId != null) {
            guidanceCheckInService.markConsumedByNote(userId, guidanceCheckInId, result.noteId());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "/image-note-jobs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageNoteEnqueueResponse> startImageNoteJob(
            HttpServletRequest request,
            @Valid @RequestBody ImageNoteStartRequest body
    ) {
        Long userId = userService.getLoginUser(request).getId();
        return ResponseEntity.ok(imageNoteJobService.enqueue(userId, body.sourceText()));
    }

    @GetMapping("/image-note-jobs/{jobId}")
    public ResponseEntity<ImageNoteStatusResponse> getImageNoteJob(
            HttpServletRequest request,
            @PathVariable String jobId
    ) {
        Long userId = userService.getLoginUser(request).getId();
        return imageNoteJobService.status(userId, jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/image-download/{fileName}")
    public ResponseEntity<Resource> downloadImage(HttpServletRequest request, @PathVariable String fileName)
            throws MalformedURLException {
        Long userId = userService.getLoginUser(request).getId();
        if (!ImageNoteJobService.SAFE_IMAGE_FILENAME.matcher(fileName).matches()) {
            return ResponseEntity.notFound().build();
        }
        if (!imageNoteJobService.ownsImage(userId, fileName)) {
            return ResponseEntity.notFound().build();
        }
        final Path path;
        try {
            path = imageNoteStorageService.resolveImagePath(fileName);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri()));
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(Objects.requireNonNull(MediaType.IMAGE_PNG))
                .body(resource);
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
    public ResponseEntity<Resource> download(
            HttpServletRequest request,
            @PathVariable String fileName
    ) throws MalformedURLException {
        Long userId = userService.getLoginUser(request).getId();
        if (!noteJobRepository.existsByUserIdAndMarkdownFileName(userId, fileName)) {
            return ResponseEntity.notFound().build();
        }
        final Path path;
        try {
            path = markdownService.resolveMarkdownPath(fileName);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
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
