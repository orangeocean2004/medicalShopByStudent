package com.medshop.consult.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude 中转站客户端（provider=claude）。
 *
 * <p>按 OpenAI 兼容格式调用中转站的 {@code POST {baseUrl}/chat/completions}：
 * 市面上主流 Claude 中转站均兼容该格式，只需在 application.yml 填入
 * {@code base-url / api-key / model} 即可切换为真实大模型。
 *
 * <p>采用 RAG：把检索到的知识片段拼入 system 提示，约束模型「依据给定资料作答、
 * 不得编造」（C-2 防幻觉）。
 */
@Component
public class ClaudeLlmClient implements LlmClient {

    private static final String SYSTEM_PROMPT =
            "你是一名严谨的执业药师助手。请仅依据【参考资料】回答用户的用药咨询，"
            + "用通俗中文给出适应症、用法用量与禁忌提示，并提醒遵医嘱。"
            + "若参考资料不足以回答，请直说「建议咨询药师或线下就医」，不要编造信息。";

    private final AiProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeLlmClient(AiProperties props) {
        this.props = props;
    }

    @Override
    public String complete(String userQuery, List<String> knowledge) {
        String reference = (knowledge == null || knowledge.isEmpty())
                ? "（无相关资料）"
                : String.join("\n", knowledge);
        String userContent = "【参考资料】\n" + reference + "\n\n【用户咨询】\n" + userQuery;

        // 构造 OpenAI 兼容请求体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userContent));
        body.put("messages", messages);
        body.put("temperature", 0.3);

        String baseUrl = props.getBaseUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String response = RestClient.create()
                .post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractContent(response);
    }

    /** 从 OpenAI 兼容响应中取 choices[0].message.content。 */
    private String extractContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            throw new IllegalStateException("响应缺少 choices[0].message.content：" + response);
        } catch (Exception e) {
            throw new RuntimeException("解析大模型响应失败：" + e.getMessage(), e);
        }
    }
}
