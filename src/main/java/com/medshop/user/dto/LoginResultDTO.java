package com.medshop.user.dto;

/**
 * 登录响应。对应 API-01 返回参数（data 内）。
 */
public class LoginResultDTO {

    private String token;
    private Long userId;
    private Integer role;

    public LoginResultDTO(String token, Long userId, Integer role) {
        this.token = token;
        this.userId = userId;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }
}
