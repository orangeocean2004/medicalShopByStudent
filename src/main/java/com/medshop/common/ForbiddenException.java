package com.medshop.common;

/**
 * 越权异常：已登录但角色无权访问该资源时抛出（如消费者访问管理后台）。
 * 由 {@link GlobalExceptionHandler} 转为 HTTP 403。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
