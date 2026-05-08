package com.aizeronote.service;

import com.aizeronote.config.AppStorageProperties;
import com.aizeronote.model.NoteSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class MarkdownService {

    private final Path outputDir;

    public MarkdownService(AppStorageProperties appStorageProperties) {
        this.outputDir = Path.of(appStorageProperties.outputDir()).toAbsolutePath();
        try {
            Files.createDirectories(this.outputDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create output directory: " + this.outputDir, ex);
        }
    }

    public GeneratedMarkdown generate(String sourceFilename, String transcription, NoteSummary summary) {
        String noteId = UUID.randomUUID().toString();
        String safeName = sanitizeFilename(sourceFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = safeName + "-" + timestamp + ".md";

        String markdown = toMarkdown(sourceFilename, transcription, summary);
        Path filePath = outputDir.resolve(fileName);
        try {
            Files.writeString(filePath, markdown, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write markdown file: " + fileName, ex);
        }

        return new GeneratedMarkdown(noteId, filePath, markdown);
    }

    public Path resolveMarkdownPath(String fileName) {
        Path resolved = outputDir.resolve(fileName).normalize();
        if (!resolved.startsWith(outputDir)) {
            throw new IllegalArgumentException("Invalid markdown file path.");
        }
        return resolved;
    }

    private String sanitizeFilename(String sourceFilename) {
        String baseName = StringUtils.hasText(sourceFilename) ? sourceFilename : "audio";
        baseName = baseName.replaceAll("\\.[^.]+$", "");
        return baseName.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private String toMarkdown(String sourceFilename, String transcription, NoteSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(summary.title()).append("\n\n");
        builder.append("**Source**: ").append(sourceFilename).append("\n\n");
        builder.append("## Abstract\n").append(summary.abstractText()).append("\n\n");
        builder.append("## Key Points\n");
        summary.keyPoints().forEach(point -> builder.append("- ").append(point).append("\n"));
        builder.append("\n## Code Snippets\n");
        if (summary.codeSnippets().isEmpty()) {
            builder.append("- None\n");
        } else {
            summary.codeSnippets().forEach(snippet -> builder.append("```text\n")
                    .append(snippet)
                    .append("\n```\n"));
        }
        builder.append("\n## Todos\n");
        summary.todos().forEach(todo -> builder.append("- [ ] ").append(todo).append("\n"));
        builder.append("\n## Full Transcription\n\n");
        builder.append(transcription).append("\n");
        return builder.toString();
    }

    public record GeneratedMarkdown(String noteId, Path filePath, String markdownPreview) {
    }
}
