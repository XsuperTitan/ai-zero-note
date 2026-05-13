package com.aizeronote.common;

public record ApiResult<T>(int code, T data, String message) {

    private static final int SUCCESS_CODE = 0;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(SUCCESS_CODE, data, "ok");
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(SUCCESS_CODE, null, "ok");
    }
}
