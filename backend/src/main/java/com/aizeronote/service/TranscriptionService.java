package com.aizeronote.service;

import com.aizeronote.config.AsrProperties;
import com.aizeronote.model.AsrStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
public class TranscriptionService {

    private final ObjectMapper objectMapper;
    private final AsrProperties asrProperties;
    private final RestClient.Builder restClientBuilder;

    public TranscriptionService(
            ObjectMapper objectMapper,
            AsrProperties asrProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.objectMapper = objectMapper;
        this.asrProperties = asrProperties;
        this.restClientBuilder = restClientBuilder;
    }

    public String transcribeWithDefaultProvider(MultipartFile file) {
        String provider = normalizeProvider(asrProperties.defaultProvider());
        return switch (provider) {
            case "challenger" -> transcribeWithChallenger(file);
            case "whisper" -> transcribeWithWhisper(file);
            case "auto" -> transcribeAuto(file);
            default -> throw new IllegalArgumentException("Unsupported ASR provider: " + provider);
        };
    }

    public AsrStatus getAsrStatus() {
        return new AsrStatus(
                normalizeProvider(asrProperties.defaultProvider()),
                isConfigured(asrProperties.whisper()),
                isConfigured(asrProperties.challenger())
        );
    }

    public String transcribeWithWhisper(MultipartFile file) {
        return transcribe(file, asrProperties.whisper(), "Whisper");
    }

    public String transcribeWithChallenger(MultipartFile file) {
        return transcribe(file, asrProperties.challenger(), "Challenger ASR");
    }

    private String transcribe(MultipartFile file, AsrProperties.ProviderConfig providerConfig, String providerName) {
        String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "audio.mp3";

        if (providerConfig == null
                || !StringUtils.hasText(providerConfig.baseUrl())
                || !StringUtils.hasText(providerConfig.apiKey())
                || !StringUtils.hasText(providerConfig.model())
                || !StringUtils.hasText(providerConfig.transcriptionPath())) {
            throw new IllegalStateException(providerName + " is not fully configured.");
        }

        String baseUrl = Objects.requireNonNull(providerConfig.baseUrl());
        String apiKey = Objects.requireNonNull(providerConfig.apiKey());
        String model = Objects.requireNonNull(providerConfig.model());
        String transcriptionPath = Objects.requireNonNull(providerConfig.transcriptionPath());

        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("model", model);
            builder.part("response_format", "json");
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            }).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.formData()
                    .name("file")
                    .filename(filename)
                    .build()
                    .toString());

            String response = restClient.post()
                    .uri(transcriptionPath)
                    .contentType(Objects.requireNonNull(MediaType.MULTIPART_FORM_DATA))
                    .body(builder.build())
                    .retrieve()
                    .body(String.class);

            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("text").asText("");
        } catch (Exception ex) {
            throw new IllegalStateException(providerName + " transcription failed: " + ex.getMessage(), ex);
        }
    }

    private String transcribeAuto(MultipartFile file) {
        boolean challengerConfigured = isConfigured(asrProperties.challenger());
        boolean whisperConfigured = isConfigured(asrProperties.whisper());

        if (challengerConfigured) {
            return transcribeWithChallenger(file);
        }
        if (whisperConfigured) {
            return transcribeWithWhisper(file);
        }
        throw new IllegalStateException("No ASR provider configured. Please set StepFun or Whisper API settings.");
    }

    private boolean isConfigured(AsrProperties.ProviderConfig providerConfig) {
        return providerConfig != null
                && StringUtils.hasText(providerConfig.baseUrl())
                && StringUtils.hasText(providerConfig.apiKey())
                && StringUtils.hasText(providerConfig.model())
                && StringUtils.hasText(providerConfig.transcriptionPath());
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "auto";
        }
        return provider.trim().toLowerCase();
    }
}
