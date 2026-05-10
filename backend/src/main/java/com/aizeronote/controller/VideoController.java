package com.aizeronote.controller;

import com.aizeronote.model.VideoFrameResult;
import com.aizeronote.model.VideoMetaResult;
import com.aizeronote.model.VideoTextResult;
import com.aizeronote.service.VideoLinkParseService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Objects;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoLinkParseService videoLinkParseService;

    public VideoController(VideoLinkParseService videoLinkParseService) {
        this.videoLinkParseService = videoLinkParseService;
    }

    @GetMapping("/meta")
    public ResponseEntity<VideoMetaResult> getMeta(@RequestParam("url") String url) {
        return ResponseEntity.ok(videoLinkParseService.getVideoMeta(url));
    }

    @GetMapping("/frames")
    public ResponseEntity<VideoFrameResult> extractFrames(@RequestParam("url") String url) {
        return ResponseEntity.ok(videoLinkParseService.extractKeyFrames(url));
    }

    @GetMapping("/text")
    public ResponseEntity<VideoTextResult> buildVideoText(@RequestParam("url") String url) {
        return ResponseEntity.ok(videoLinkParseService.buildVideoText(url));
    }

    @GetMapping("/download/{taskId}/{fileName}")
    public ResponseEntity<Resource> download(
            @PathVariable String taskId,
            @PathVariable String fileName
    ) throws MalformedURLException {
        Path path = videoLinkParseService.resolveFramePath(taskId, fileName);
        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri()));
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(Objects.requireNonNull(MediaType.IMAGE_JPEG))
                .body(resource);
    }
}
