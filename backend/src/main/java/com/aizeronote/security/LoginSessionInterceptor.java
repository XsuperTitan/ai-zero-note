package com.aizeronote.security;

import com.aizeronote.exception.ErrorCode;
import com.aizeronote.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class LoginSessionInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public LoginSessionInterceptor(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (userService.isLoggedIn(request)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorCode code = ErrorCode.NOT_LOGIN_ERROR;
        Map<String, Object> body = Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "code", code.getCode(),
                "error", code.getDefaultMessage()
        );
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }
}
