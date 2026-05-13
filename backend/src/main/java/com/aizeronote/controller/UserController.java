package com.aizeronote.controller;

import com.aizeronote.common.ApiResult;
import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.dto.user.UserLoginRequest;
import com.aizeronote.model.dto.user.UserRegisterRequest;
import com.aizeronote.model.entity.User;
import com.aizeronote.model.vo.LoginUserVO;
import com.aizeronote.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResult<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        long id = userService.register(request);
        return ApiResult.ok(id);
    }

    @PostMapping("/login")
    public ApiResult<LoginUserVO> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginUserVO vo = userService.login(request, httpRequest);
        return ApiResult.ok(vo);
    }

    @GetMapping("/get/login")
    public ApiResult<LoginUserVO> getLoginUser(HttpServletRequest httpRequest) {
        User user = userService.getLoginUser(httpRequest);
        LoginUserVO vo = userService.getLoginUserVO(user);
        return ApiResult.ok(vo);
    }

    @PostMapping("/logout")
    public ApiResult<Boolean> logout(HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean ok = userService.logout(httpRequest);
        return ApiResult.ok(ok);
    }
}
