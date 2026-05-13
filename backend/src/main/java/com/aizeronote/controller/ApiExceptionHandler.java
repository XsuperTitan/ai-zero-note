package com.aizeronote.controller;

import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        String message = ex.getMessage() != null ? ex.getMessage() : code.getDefaultMessage();
        return ResponseEntity.status(code.getHttpStatus()).body(errorBody(code.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : ErrorCode.PARAMS_ERROR.getDefaultMessage())
                .orElse(ErrorCode.PARAMS_ERROR.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ErrorCode.PARAMS_ERROR.getCode(), message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ErrorCode.PARAMS_ERROR.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleProcessing(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "Processing failed" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorBody(ErrorCode.SYSTEM_ERROR.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled server error", ex);
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(ErrorCode.SYSTEM_ERROR.getCode(), "Internal error: " + message));
    }

    private Map<String, Object> errorBody(int code, String message) {
        return Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "code", code,
                "error", message == null ? "" : message
        );
    }
}
