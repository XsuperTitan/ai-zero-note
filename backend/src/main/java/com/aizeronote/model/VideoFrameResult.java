package com.aizeronote.model;

import java.util.List;

public record VideoFrameResult(
        String taskId,
        VideoMetaResult meta,
        List<VideoFrameItem> frames
) {
}
