package com.medshop.common;

/**
 * 业务异常。Service 层抛出，由 {@link GlobalExceptionHandler} 统一捕获并转为
 * {@code {code, message}} 返回，避免把堆栈暴露给前端。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(1, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
