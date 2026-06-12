package com.medshop.consult.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 咨询消息实体，对应 consultation_message 表（FP-AI-05 留存，可追溯）。
 */
public class ConsultationMessage {

    private Long id;
    private Long consultationId;
    private Integer senderType;    // 0用户 1AI 2药师
    private String content;
    private String aiSource;       // AI 回复的知识来源标识（C-2）
    private BigDecimal confidence; // AI 置信度
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public Integer getSenderType() {
        return senderType;
    }

    public void setSenderType(Integer senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAiSource() {
        return aiSource;
    }

    public void setAiSource(String aiSource) {
        this.aiSource = aiSource;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
