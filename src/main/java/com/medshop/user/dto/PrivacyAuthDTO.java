package com.medshop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 隐私授权管理请求。对应 API-10 请求参数（FP-REC-04，约束 C-3）。
 */
public class PrivacyAuthDTO {

    @NotBlank(message = "授权范围不能为空")
    private String scope;          // recommendation / healthProfile

    @NotNull(message = "授权状态不能为空")
    private Integer status;        // 1授权 0撤回

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
}
