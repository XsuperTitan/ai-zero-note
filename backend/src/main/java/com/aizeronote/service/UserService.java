package com.aizeronote.service;

import com.aizeronote.model.dto.user.UserLoginRequest;
import com.aizeronote.model.dto.user.UserRegisterRequest;
import com.aizeronote.model.entity.User;
import com.aizeronote.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    long register(UserRegisterRequest request);

    LoginUserVO login(UserLoginRequest request, HttpServletRequest httpRequest);

    boolean logout(HttpServletRequest httpRequest);

    User getLoginUser(HttpServletRequest httpRequest);

    LoginUserVO getLoginUserVO(User user);

    boolean isLoggedIn(HttpServletRequest httpRequest);
}
