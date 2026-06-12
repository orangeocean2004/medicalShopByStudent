package com.medshop.consult.dto;

import java.math.BigDecimal;

/**
 * AI 回复响应。对应 API-06 返回参数（data 内）。
 */
public class AiReplyDTO {

    private String reply;
    private String aiSource;
    private BigDecimal confidence;
    private boolean needHandoff;   // 前端据此决定是否展示「一键转人工」
    private boolean handoffMode;   // true=会话已转人工，AI 退出，由药师接管

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
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

    public boolean isNeedHandoff() {
        return needHandoff;
    }

    public void setNeedHandoff(boolean needHandoff) {
        this.needHandoff = needHandoff;
    }

    public boolean isHandoffMode() {
        return handoffMode;
    }

    public void setHandoffMode(boolean handoffMode) {
        this.handoffMode = handoffMode;
    }
}
