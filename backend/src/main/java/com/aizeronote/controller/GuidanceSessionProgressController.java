package com.aizeronote.controller;

import com.aizeronote.common.ApiResult;
import com.aizeronote.model.dto.guidance.GuidanceCurrentVideoUpdateRequest;
import com.aizeronote.service.UserService;
import com.aizeronote.service.guidance.GuidanceSessionProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guidance/session")
@Validated
public class GuidanceSessionProgressController {

    private final UserService userService;
    private final GuidanceSessionProgressService guidanceSessionProgressService;

    public GuidanceSessionProgressController(
            UserService userService,
            GuidanceSessionProgressService guidanceSessionProgressService
    ) {
        this.userService = userService;
        this.guidanceSessionProgressService = guidanceSessionProgressService;
    }

    @PostMapping("/{sessionId}/enter-in-progress")
    public ApiResult<Boolean> enterInProgress(HttpServletRequest request, @PathVariable long sessionId) {
        long userId = userService.getLoginUser(request).getId();
        guidanceSessionProgressService.enterInProgress(userId, sessionId);
        return ApiResult.ok(true);
    }

    @PatchMapping("/{sessionId}/current-video")
    public ApiResult<Boolean> updateCurrentVideo(
            HttpServletRequest request,
            @PathVariable long sessionId,
            @Valid @RequestBody GuidanceCurrentVideoUpdateRequest body
    ) {
        long userId = userService.getLoginUser(request).getId();
        guidanceSessionProgressService.updateCurrentVideo(userId, sessionId, body.getCurrentVideoId());
        return ApiResult.ok(true);
    }

    @PostMapping("/{sessionId}/complete")
    public ApiResult<Boolean> complete(HttpServletRequest request, @PathVariable long sessionId) {
        long userId = userService.getLoginUser(request).getId();
        guidanceSessionProgressService.complete(userId, sessionId);
        return ApiResult.ok(true);
    }
}
