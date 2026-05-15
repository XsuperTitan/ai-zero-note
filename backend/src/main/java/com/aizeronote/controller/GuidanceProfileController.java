package com.aizeronote.controller;

import com.aizeronote.common.ApiResult;
import com.aizeronote.model.dto.guidance.GuidanceProfileResponse;
import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.service.UserService;
import com.aizeronote.service.guidance.LearningProfileService;
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
@RequestMapping("/api/guidance/profile")
@Validated
public class GuidanceProfileController {

    private final UserService userService;
    private final LearningProfileService learningProfileService;

    public GuidanceProfileController(UserService userService, LearningProfileService learningProfileService) {
        this.userService = userService;
        this.learningProfileService = learningProfileService;
    }

    @PostMapping("/submit")
    public ApiResult<GuidanceProfileResponse> submit(
            HttpServletRequest request,
            @Valid @RequestBody LearningQuestionnaireSubmitRequest body
    ) {
        long userId = userService.getLoginUser(request).getId();
        GuidanceProfileResponse data = learningProfileService.submitQuestionnaire(userId, body);
        return ApiResult.ok(data);
    }

    @GetMapping("/latest")
    public ApiResult<GuidanceProfileResponse> latest(HttpServletRequest request) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(learningProfileService.getLatest(userId));
    }

    @GetMapping("/{sessionId}")
    public ApiResult<GuidanceProfileResponse> byId(HttpServletRequest request, @PathVariable long sessionId) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(learningProfileService.getById(userId, sessionId));
    }
}
