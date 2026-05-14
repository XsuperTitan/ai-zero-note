package com.aizeronote.controller;

import com.aizeronote.model.VideoFrameResult;
import com.aizeronote.model.VideoMetaResult;
import com.aizeronote.model.VideoTextResult;
import com.aizeronote.model.VideoVisionResult;
import com.aizeronote.repository.VideoTaskRepository;
import com.aizeronote.service.UserService;
import com.aizeronote.service.VideoLinkParseService;
import com.aizeronote.service.VideoTaskService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoLinkParseService videoLinkParseService;
    private final UserService userService;
    private final VideoTaskService videoTaskService;
    private final VideoTaskRepository videoTaskRepository;

    public VideoController(
            VideoLinkParseService videoLinkParseService,
            UserService userService,
            VideoTaskService videoTaskService,
            VideoTaskRepository videoTaskRepository
    ) {
        this.videoLinkParseService = videoLinkParseService;
        this.userService = userService;
        this.videoTaskService = videoTaskService;
        this.videoTaskRepository = videoTaskRepository;
    }

    @GetMapping("/meta")
    public ResponseEntity<VideoMetaResult> getMeta(@RequestParam("url") String url) {
        return ResponseEntity.ok(videoLinkParseService.getVideoMeta(url));
    }

    @GetMapping("/frames")
    public ResponseEntity<VideoFrameResult> extractFrames(HttpServletRequest request, @RequestParam("url") String url) {
        Long userId = userService.getLoginUser(request).getId();
        VideoFrameResult result = videoLinkParseService.extractKeyFrames(url);
        videoTaskService.tryPersist(userId, url, result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/text")
    public ResponseEntity<VideoTextResult> buildVideoText(@RequestParam("url") String url) {
        return ResponseEntity.ok(videoLinkParseService.buildVideoText(url));
    }

    @GetMapping("/vision-text")
    public ResponseEntity<VideoVisionResult> buildVisionText(
            @RequestParam("url") String url,
            @RequestParam("taskId") String taskId,
            @RequestParam("fileName") List<String> fileNames,
            @RequestParam(value = "targetLanguage", required = false) String targetLanguage
    ) {
        return ResponseEntity.ok(videoLinkParseService.buildVisionText(url, taskId, fileNames, targetLanguage));
    }

    @GetMapping("/download/{taskId}/{fileName}")
    public ResponseEntity<Resource> download(
            HttpServletRequest request,
            @PathVariable String taskId,
            @PathVariable String fileName
    ) throws MalformedURLException {
        Long userId = userService.getLoginUser(request).getId();
        if (!videoTaskRepository.existsByUserIdAndTaskId(userId, taskId)) {
            return ResponseEntity.notFound().build();
        }
        final Path path;
        try {
            path = videoLinkParseService.resolveFramePath(taskId, fileName);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
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
