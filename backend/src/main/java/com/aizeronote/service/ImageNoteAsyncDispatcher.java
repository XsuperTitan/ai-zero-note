package com.aizeronote.service;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

@Component
public class ImageNoteAsyncDispatcher {

    private final ImageNoteGenerationProcessor processor;

    public ImageNoteAsyncDispatcher(ImageNoteGenerationProcessor processor) {
        this.processor = processor;
    }

    @Async("taskExecutor")
    public void dispatch(String jobId) {
        processor.process(jobId);
    }
}
