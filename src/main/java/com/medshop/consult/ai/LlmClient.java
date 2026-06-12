package com.medshop.consult.ai;

import java.util.List;

/**
 * 大模型调用客户端抽象。P1 中台通过本接口屏蔽底层模型差异：
 * 离线时用 {@link MockLlmClient}，联网时用 {@link ClaudeLlmClient}（Claude 中转站）。
 */
public interface LlmClient {

    /**
     * 基于检索到的知识与用户问题生成回复。
     *
     * @param userQuery     用户问题
     * @param knowledge     RAG 检索命中的知识片段（已按相关度排序）
     * @return 生成的用药建议文本
     */
    String complete(String userQuery, List<String> knowledge);
}
