package com.medshop.consult.ai;

import java.math.BigDecimal;

/**
 * AI 中台生成结果（P1 的统一输出）。
 *
 * <p>封装大模型/知识库生成的回复及其合规与风险元数据，供业务层据此决定是否转人工。
 * 对应《代码设计》中 AiResult 的字段：reply、aiSource、confidence、riskLevel、blockedByCompliance。
 */
public class AiResult {

    /** AI 用药建议正文。 */
    private String reply;
    /** 知识来源标识（RAG 命中的条目标题/来源），约束 C-2。 */
    private String aiSource;
    /** 置信度 0~1。 */
    private BigDecimal confidence;
    /** 风险等级，约束 C-1。 */
    private RiskLevel riskLevel;
    /** 是否被合规过滤拦截。 */
    private boolean blockedByCompliance;

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

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isBlockedByCompliance() {
        return blockedByCompliance;
    }

    public void setBlockedByCompliance(boolean blockedByCompliance) {
        this.blockedByCompliance = blockedByCompliance;
    }
}
