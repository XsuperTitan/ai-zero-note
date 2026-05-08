package com.aizeronote.service;

import com.aizeronote.model.AccuracyMetrics;
import com.aizeronote.model.TranscriptionComparisonResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Service
public class TranscriptionComparisonService {

    private final TranscriptionService transcriptionService;

    public TranscriptionComparisonService(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    public TranscriptionComparisonResult compare(MultipartFile file, String referenceText) {
        String whisper = transcriptionService.transcribeWithWhisper(file);
        String challenger = transcriptionService.transcribeWithChallenger(file);

        if (!StringUtils.hasText(referenceText)) {
            return new TranscriptionComparisonResult(
                    file.getOriginalFilename(),
                    whisper,
                    challenger,
                    new AccuracyMetrics(null, null, "unknown"),
                    "No reference text provided, cannot compute objective accuracy (WER)."
            );
        }

        double whisperWer = computeWer(referenceText, whisper);
        double challengerWer = computeWer(referenceText, challenger);
        String betterProvider = whisperWer <= challengerWer ? "whisper" : "challenger";

        return new TranscriptionComparisonResult(
                file.getOriginalFilename(),
                whisper,
                challenger,
                new AccuracyMetrics(round4(whisperWer), round4(challengerWer), betterProvider),
                "Lower WER means better transcription accuracy."
        );
    }

    private double computeWer(String reference, String hypothesis) {
        String[] refWords = tokenize(reference);
        String[] hypWords = tokenize(hypothesis);
        if (refWords.length == 0) {
            return hypWords.length == 0 ? 0.0 : 1.0;
        }
        int distance = levenshteinDistance(refWords, hypWords);
        return (double) distance / refWords.length;
    }

    private String[] tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase().replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(normalized.split(" "))
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    private int levenshteinDistance(String[] ref, String[] hyp) {
        int[][] dp = new int[ref.length + 1][hyp.length + 1];
        for (int i = 0; i <= ref.length; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= hyp.length; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= ref.length; i++) {
            for (int j = 1; j <= hyp.length; j++) {
                int cost = ref[i - 1].equals(hyp[j - 1]) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[ref.length][hyp.length];
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
