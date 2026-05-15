package com.aizeronote.controller;

import com.aizeronote.common.ApiResult;
import com.aizeronote.model.dto.guidance.StudyPlanGenerateRequest;
import com.aizeronote.model.dto.guidance.StudyPlanResponse;
import com.aizeronote.service.UserService;
import com.aizeronote.service.guidance.GuidanceStudyPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guidance/plan")
@Validated
public class GuidanceStudyPlanController {

    private final UserService userService;
    private final GuidanceStudyPlanService guidanceStudyPlanService;

    public GuidanceStudyPlanController(UserService userService, GuidanceStudyPlanService guidanceStudyPlanService) {
        this.userService = userService;
        this.guidanceStudyPlanService = guidanceStudyPlanService;
    }

    @PostMapping("/generate")
    public ApiResult<StudyPlanResponse> generate(
            HttpServletRequest request,
            @Valid @RequestBody StudyPlanGenerateRequest body
    ) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(guidanceStudyPlanService.generateStudyPlan(userId, body));
    }

    @GetMapping("/latest")
    public ApiResult<StudyPlanResponse> latest(HttpServletRequest request) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(guidanceStudyPlanService.getLatest(userId));
    }

    @GetMapping("/session/{sessionId}")
    public ApiResult<StudyPlanResponse> bySession(HttpServletRequest request, @PathVariable long sessionId) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(guidanceStudyPlanService.getBySessionId(userId, sessionId));
    }
}
