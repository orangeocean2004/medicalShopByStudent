package com.medshop.consult.entity;

import java.time.LocalDateTime;

/**
 * 人工兜底工单实体，对应 handoff_ticket 表（公共模块 P2，约束 C-4）。
 */
public class HandoffTicket {

    private Long id;
    private Integer sourceType;    // 1药师咨询 2客服 3处方
    private Long sourceId;
    private Long userId;
    private Long agentId;          // 接手坐席
    private String context;        // 携带的对话上下文 JSON
    private String reason;
    private Integer status;        // 0待接 1处理中 2已完成
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
