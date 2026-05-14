package com.aizeronote.controller;

import com.aizeronote.model.NoteStyle;
import com.aizeronote.model.OutputLanguage;
import org.springframework.util.StringUtils;

public final class NoteEnumsParser {

    private NoteEnumsParser() {
    }

    public static NoteStyle parseNoteStyle(String raw, NoteStyle defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return NoteStyle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid noteStyle: " + raw);
        }
    }

    public static OutputLanguage parseOutputLanguage(String raw, OutputLanguage defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return OutputLanguage.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid outputLanguage: " + raw);
        }
    }
}
