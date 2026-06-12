package com.medshop.consult.ai;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 离线模拟大模型客户端（provider=mock，默认）。
 *
 * <p>不联网、不需要 API Key：直接把 RAG 命中的权威知识片段组织成一段用药建议，
 * 并附标准就医提示。这样在无网络环境也能完整演示「RAG → 生成 → 置信判定 → 转人工」全流程。
 */
@Component
public class MockLlmClient implements LlmClient {

    @Override
    public String complete(String userQuery, List<String> knowledge) {
        StringBuilder sb = new StringBuilder();
        if (knowledge == null || knowledge.isEmpty()) {
            // 知识库未命中：不臆造，给出兜底话术（配合低置信触发转人工，约束 C-2 防幻觉）
            return "根据现有药学知识库，暂未检索到与您描述高度匹配的权威用药信息。"
                    + "为保证安全，建议您咨询执业药师或线下就医，避免自行用药。";
        }
        sb.append("根据药学知识库为您整理如下用药建议：\n");
        for (String k : knowledge) {
            sb.append("· ").append(k).append("\n");
        }
        sb.append("温馨提示：以上为基于权威资料的参考建议，具体用药请遵医嘱；若症状持续或加重，请及时就医。");
        return sb.toString();
    }
}
