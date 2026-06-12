package com.medshop.common;

/**
 * 未鉴权异常：请求缺少有效令牌或令牌过期时抛出，由网关层（拦截器）触发。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
