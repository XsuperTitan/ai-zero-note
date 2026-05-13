package com.aizeronote.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PARAMS_ERROR(40000, HttpStatus.BAD_REQUEST, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, HttpStatus.UNAUTHORIZED, "未登录"),
    NO_AUTH_ERROR(40101, HttpStatus.FORBIDDEN, "无权限"),
    OPERATION_ERROR(50001, HttpStatus.BAD_REQUEST, "操作失败"),
    SYSTEM_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "系统错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
