package com.aizeronote.controller;

import com.aizeronote.common.ApiResult;
import com.aizeronote.model.dto.guidance.GuidanceActiveProgressResponse;
import com.aizeronote.service.UserService;
import com.aizeronote.service.guidance.GuidanceSessionProgressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guidance/progress")
public class GuidanceProgressController {

    private final UserService userService;
    private final GuidanceSessionProgressService guidanceSessionProgressService;

    public GuidanceProgressController(
            UserService userService,
            GuidanceSessionProgressService guidanceSessionProgressService
    ) {
        this.userService = userService;
        this.guidanceSessionProgressService = guidanceSessionProgressService;
    }

    /**
     * Latest plan-backed session in {@code PLAN_READY} or {@code IN_PROGRESS} for dashboard / resume UI.
     */
    @GetMapping("/active")
    public ApiResult<GuidanceActiveProgressResponse> active(HttpServletRequest request) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(guidanceSessionProgressService.findActiveProgress(userId).orElse(null));
    }
}
