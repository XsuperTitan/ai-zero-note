package com.aizeronote.service;

import com.aizeronote.config.AppStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class ImageNoteStorageService {

    static final String SUBDIR = "image-notes";

    private final Path imageDir;

    public ImageNoteStorageService(AppStorageProperties appStorageProperties) {
        this.imageDir = Path.of(appStorageProperties.outputDir()).toAbsolutePath().resolve(SUBDIR);
        try {
            Files.createDirectories(this.imageDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create image output directory: " + this.imageDir, ex);
        }
    }

    public Path resolveImagePath(String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid image file name.");
        }
        Path resolved = imageDir.resolve(fileName).normalize();
        if (!resolved.startsWith(imageDir)) {
            throw new IllegalArgumentException("Invalid image file path.");
        }
        return resolved;
    }

    public void writeBytes(String fileName, byte[] pngBytes) throws IOException {
        Files.write(
                resolveImagePath(fileName),
                pngBytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
