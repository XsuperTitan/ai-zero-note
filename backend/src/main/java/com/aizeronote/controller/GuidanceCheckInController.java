package com.aizeronote.controller;

import com.aizeronote.common.ApiResult;
import com.aizeronote.model.dto.guidance.GuidanceCheckInCreateRequest;
import com.aizeronote.model.dto.guidance.GuidanceCheckInResponse;
import com.aizeronote.model.dto.guidance.GuidanceCheckInSupplementRequest;
import com.aizeronote.service.UserService;
import com.aizeronote.service.guidance.GuidanceCheckInService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guidance/check-in")
@Validated
public class GuidanceCheckInController {

    private final UserService userService;
    private final GuidanceCheckInService guidanceCheckInService;

    public GuidanceCheckInController(UserService userService, GuidanceCheckInService guidanceCheckInService) {
        this.userService = userService;
        this.guidanceCheckInService = guidanceCheckInService;
    }

    @PostMapping
    public ApiResult<GuidanceCheckInResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody GuidanceCheckInCreateRequest body
    ) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(guidanceCheckInService.create(userId, body));
    }

    @PostMapping("/{checkInId}/supplement")
    public ApiResult<GuidanceCheckInResponse> supplement(
            HttpServletRequest request,
            @PathVariable long checkInId,
            @Valid @RequestBody GuidanceCheckInSupplementRequest body
    ) {
        long userId = userService.getLoginUser(request).getId();
        return ApiResult.ok(guidanceCheckInService.addSupplement(userId, checkInId, body));
    }
}
