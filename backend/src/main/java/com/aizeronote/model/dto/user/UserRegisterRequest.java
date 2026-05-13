package com.aizeronote.model.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserRegisterRequest(
        @NotBlank(message = "账号不能为空") String userAccount,
        @NotBlank(message = "密码不能为空") String userPassword,
        @NotBlank(message = "确认密码不能为空") String checkPassword,
        @NotBlank(message = "邀请码不能为空") String inviteCode
) {}
