package com.medshop.consult.ai;

import com.medshop.consult.entity.KnowledgeItem;
import com.medshop.consult.mapper.KnowledgeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiGatewayService 单元测试（P1 AI 能力中台）：
 * RAG 命中→置信度梯度、未命中低置信、高风险词判定、合规黑名单拦截、模型异常降级。
 * KnowledgeMapper 与两个 LlmClient 用 Mockito 桩，避免真实模型/数据库依赖。
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayServiceTest {

    @Mock KnowledgeMapper knowledgeMapper;
    @Mock MockLlmClient mockClient;
    @Mock ClaudeLlmClient claudeClient;

    AiProperties props;
    AiGatewayService gateway;

    @BeforeEach
    void setUp() {
        props = new AiProperties();
        props.setProvider("mock"); // 默认走 mock 客户端
        gateway = new AiGatewayService(knowledgeMapper, mockClient, claudeClient, props);
    }

    private KnowledgeItem ki(long id, String title, String keywords) {
        KnowledgeItem k = new KnowledgeItem();
        k.setId(id);
        k.setTitle(title);
        k.setContent("内容-" + title);
        k.setKeywords(keywords);
        return k;
    }

    // ---------- RAG 命中与置信度 ----------

    @Test
    @DisplayName("单关键词命中：置信度=0.60，风险中等，来源回填命中条目标题")
    void generate_singleHit_confidence060() {
        // 标题与query无重叠，仅靠keyword命中1分 → 0.60（避免标题bigram额外加分）
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "用药须知", "感冒")));
        when(mockClient.complete(anyString(), anyList())).thenReturn("多喝水，注意休息。");

        AiResult r = gateway.generate("感冒");

        assertEquals(0, new BigDecimal("0.60").compareTo(r.getConfidence()));
        assertEquals(RiskLevel.MEDIUM, r.getRiskLevel());
        assertFalse(r.isBlockedByCompliance());
        assertTrue(r.getAiSource().contains("用药须知"));
    }

    @Test
    @DisplayName("多关键词命中：命中越多置信度越高（2命中→0.75），封顶0.95")
    void generate_multiHit_higherConfidence() {
        // 同一条目内多个关键词都命中 query
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "布洛芬用药", "布洛芬,发烧,退烧")));
        when(mockClient.complete(anyString(), anyList())).thenReturn("按说明服用。");

        AiResult r = gateway.generate("布洛芬能退烧吗"); // 命中 布洛芬+退烧 至少2项

        assertTrue(r.getConfidence().compareTo(new BigDecimal("0.60")) > 0,
                "多命中置信度应高于单命中");
        assertTrue(r.getConfidence().compareTo(new BigDecimal("0.95")) <= 0, "不超过封顶0.95");
    }

    @Test
    @DisplayName("RAG 未命中任何知识：置信度=0.30（触发业务层转人工）")
    void generate_noHit_confidence030() {
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "感冒护理", "感冒")));
        when(mockClient.complete(anyString(), anyList())).thenReturn("通用建议。");

        AiResult r = gateway.generate("完全不相关的天气问题xyz");

        assertEquals(0, new BigDecimal("0.30").compareTo(r.getConfidence()));
    }

    // ---------- 风险判定（C-1）----------

    @Test
    @DisplayName("命中高风险词（孕妇）：风险等级HIGH")
    void generate_highRiskKeyword_high() {
        when(knowledgeMapper.selectAll()).thenReturn(new ArrayList<>());
        when(mockClient.complete(anyString(), anyList())).thenReturn("建议咨询医生。");

        AiResult r = gateway.generate("孕妇可以吃这个药吗");
        assertEquals(RiskLevel.HIGH, r.getRiskLevel());
    }

    @Test
    @DisplayName("命中高风险词（处方药/抗生素）：风险等级HIGH")
    void generate_highRiskRxKeyword_high() {
        when(knowledgeMapper.selectAll()).thenReturn(new ArrayList<>());
        when(mockClient.complete(anyString(), anyList())).thenReturn("请遵医嘱。");

        assertEquals(RiskLevel.HIGH, gateway.generate("抗生素怎么用").getRiskLevel());
        assertEquals(RiskLevel.HIGH, gateway.generate("这个处方药副作用").getRiskLevel());
    }

    @Test
    @DisplayName("普通咨询无高风险词：风险等级MEDIUM")
    void generate_normalQuery_medium() {
        when(knowledgeMapper.selectAll()).thenReturn(new ArrayList<>());
        when(mockClient.complete(anyString(), anyList())).thenReturn("一般护理建议。");

        assertEquals(RiskLevel.MEDIUM, gateway.generate("嗓子有点干").getRiskLevel());
    }

    // ---------- 合规过滤（C-1）----------

    @Test
    @DisplayName("模型输出含合规黑名单词（包治）：替换为合规话术、置信归零、标记blocked")
    void generate_complianceBlocked() {
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "感冒护理", "感冒")));
        when(mockClient.complete(anyString(), anyList())).thenReturn("本品包治百病无副作用");

        AiResult r = gateway.generate("感冒");

        assertTrue(r.isBlockedByCompliance());
        assertEquals(0, new BigDecimal("0.0").compareTo(r.getConfidence()));
        assertFalse(r.getReply().contains("包治"));
        assertTrue(r.getReply().contains("执业药师"));
    }

    // ---------- 模型异常降级 ----------

    @Test
    @DisplayName("模型调用抛异常：降级为低置信(0.0)+HIGH风险兜底，不把异常抛给用户")
    void generate_modelError_gracefulDegrade() {
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "感冒护理", "感冒")));
        when(mockClient.complete(anyString(), anyList())).thenThrow(new RuntimeException("LLM超时"));

        AiResult r = gateway.generate("感冒");

        assertEquals(0, new BigDecimal("0.0").compareTo(r.getConfidence()));
        assertEquals(RiskLevel.HIGH, r.getRiskLevel());
        assertFalse(r.isBlockedByCompliance());
        assertTrue(r.getReply().contains("人工药师"));
    }

    // ---------- provider 路由 ----------

    @Test
    @DisplayName("provider=claude 时改用 Claude 客户端，不调 mock 客户端")
    void generate_providerClaude_usesClaudeClient() {
        props.setProvider("claude");
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "感冒护理", "感冒")));
        when(claudeClient.complete(anyString(), anyList())).thenReturn("Claude生成的建议。");

        AiResult r = gateway.generate("感冒");

        assertEquals("Claude生成的建议。", r.getReply());
        verify(claudeClient).complete(anyString(), anyList());
        verify(mockClient, never()).complete(anyString(), anyList());
    }

    @Test
    @DisplayName("provider=mock（默认）时使用 mock 客户端，不调 Claude")
    void generate_providerMock_usesMockClient() {
        when(knowledgeMapper.selectAll()).thenReturn(List.of(ki(1L, "感冒护理", "感冒")));
        when(mockClient.complete(anyString(), anyList())).thenReturn("mock建议。");

        gateway.generate("感冒");

        verify(mockClient).complete(anyString(), anyList());
        verify(claudeClient, never()).complete(anyString(), anyList());
    }
}
