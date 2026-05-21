package com.saas.framework.common.exception;

import com.saas.framework.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 * 统一捕获所有异常，返回与 Result 一致的 JSON 格式
 * HTTP 状态码与 code 字段保持一致
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     * 动态设置 HTTP 状态码，保持与 code 字段一致
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        response.setStatus(e.getCode());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 (@Valid + JSR303)
     * 只返回第一个错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String msg = fieldError.getDefaultMessage();
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * Bind 异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String msg = fieldError.getDefaultMessage();
        log.warn("参数绑定失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数校验失败");
        log.warn("约束校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.serverError();
    }
}
