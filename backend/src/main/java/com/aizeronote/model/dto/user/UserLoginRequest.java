package com.aizeronote.model.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank(message = "账号不能为空") String userAccount,
        @NotBlank(message = "密码不能为空") String userPassword
) {}
