package com.aizeronote.model;

import java.util.List;

public record NoteResult(
        String noteId,
        String sourceFilename,
        String transcription,
        String title,
        String abstractText,
        List<String> keyPoints,
        List<String> codeSnippets,
        List<String> todos,
        String markdownPreview,
        String downloadUrl,
        NoteStyle noteStyle,
        OutputLanguage outputLanguage,
        String mindMapJson
) {
}
