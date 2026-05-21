package com.saas.framework.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于在业务逻辑中手动抛出，由全局异常处理器统一捕获并返回
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 异常状态码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
