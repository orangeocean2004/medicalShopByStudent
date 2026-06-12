package com.medshop.common;

/**
 * 统一返回结构 {@code {code, message, data}}。
 *
 * <p>对应《接口设计》中"所有接口统一返回 {code,message,data}"的约定：
 * code=0 表示成功，非 0 为业务错误码。
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（带数据）。 */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    /** 成功（无数据）。 */
    public static <T> Result<T> success() {
        return new Result<>(0, "success", null);
    }

    /** 业务错误。 */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
