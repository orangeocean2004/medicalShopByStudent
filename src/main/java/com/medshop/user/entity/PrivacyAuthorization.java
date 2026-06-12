package com.medshop.user.entity;

import java.time.LocalDateTime;

/**
 * 隐私授权实体，对应 privacy_authorization 表（FP-REC-04，约束 C-3）。
 */
public class PrivacyAuthorization {

    private Long id;
    private Long userId;
    private String scope;          // recommendation / healthProfile
    private Integer status;        // 1已授权 0已撤回
    private LocalDateTime authorizedAt;
    private LocalDateTime revokedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
