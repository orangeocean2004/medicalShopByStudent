package com.medshop.consult.ai;

/**
 * AI 风险等级。承接约束 C-1（风险熔断）。
 */
public enum RiskLevel {
    /** 低风险：常规健康咨询。 */
    LOW,
    /** 中风险：涉及具体用药建议。 */
    MEDIUM,
    /** 高风险：涉及处方药、儿童/孕妇用药、急重症等，应转人工。 */
    HIGH
}
