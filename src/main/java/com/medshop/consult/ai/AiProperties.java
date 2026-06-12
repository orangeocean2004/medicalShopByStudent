package com.medshop.consult.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * AI 中台配置（绑定 application.yml 中 medshop.ai.*）。
 */
@ConfigurationProperties(prefix = "medshop.ai")
public class AiProperties {

    /** mock=离线模拟；claude=调用 Claude 中转站。 */
    private String provider = "mock";
    private String baseUrl;
    private String apiKey;
    private String model;
    /** 置信度阈值，低于此值建议转人工。 */
    private BigDecimal confidenceThreshold = new BigDecimal("0.6");

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public BigDecimal getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(BigDecimal confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
}
