package com.medshop.consult.dto;

/**
 * 会话消息列表项（用户端 / 药师端轮询拉取对话用）。
 * senderType：0用户 1AI 2药师。
 */
public class MessageItemDTO {

    private Long id;
    private Integer senderType;
    private String content;
    private String aiSource;
    private java.math.BigDecimal confidence;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public java.math.BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(java.math.BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
