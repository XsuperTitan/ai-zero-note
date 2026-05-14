package com.aizeronote.model;

import java.util.List;

public record NoteSummary(
        String title,
        String abstractText,
        List<String> keyPoints,
        List<String> codeSnippets,
        List<String> todos,
        String markdownContent,
        String mindMapJson
) {
}
