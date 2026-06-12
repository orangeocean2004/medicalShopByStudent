package com.medshop.consult.ai;

import com.medshop.consult.entity.KnowledgeItem;
import com.medshop.consult.mapper.KnowledgeMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI 能力中台（P1 AiGatewayService）——全系统唯一直接调用大模型的地方。
 *
 * <p>统一封装：RAG 知识检索 → 大模型生成 → 合规过滤 → 置信度/风险判定，
 * 承接约束 C-1（风险熔断）、C-2（防幻觉/可追溯）。被 M2 等业务模块复用，
 * 业务层不直接碰模型。
 *
 * <p>{@code provider=mock} 时走本地知识库 RAG + 模板生成（离线可演示）；
 * {@code provider=claude} 时把检索结果交给 Claude 中转站生成。两种模式
 * 共用同一套合规与置信判定逻辑，保证口径一致。
 */
@Service
public class AiGatewayService {

    /** 合规黑名单：命中则拦截（约束 C-1，药品广告法/越界承诺等）。 */
    private static final List<String> COMPLIANCE_BLACKLIST = Arrays.asList(
            "包治", "根治", "百分百治愈", "无副作用", "替代处方", "保证治好");

    /** 高风险触发词：涉及特殊人群/急重症/处方药场景，应转人工（约束 C-1）。 */
    private static final List<String> HIGH_RISK_KEYWORDS = Arrays.asList(
            "孕妇", "怀孕", "婴儿", "新生儿", "抽搐", "昏迷", "过敏性休克",
            "大出血", "自杀", "服毒", "处方药", "抗生素", "胸痛", "呼吸困难");

    private final KnowledgeMapper knowledgeMapper;
    private final LlmClient mockClient;
    private final ClaudeLlmClient claudeClient;
    private final AiProperties props;

    public AiGatewayService(KnowledgeMapper knowledgeMapper,
                            MockLlmClient mockClient,
                            ClaudeLlmClient claudeClient,
                            AiProperties props) {
        this.knowledgeMapper = knowledgeMapper;
        this.mockClient = mockClient;
        this.claudeClient = claudeClient;
        this.props = props;
    }

    /**
     * 生成用药建议：RAG 检索 → 模型生成 → 合规过滤 → 置信/风险判定。
     */
    public AiResult generate(String query) {
        AiResult result = new AiResult();

        // 1. RAG 检索：召回 Top-K 相关知识条目（C-2 提供来源、防幻觉）
        List<ScoredItem> hits = retrieve(query, 3);
        List<String> snippets = new ArrayList<>();
        StringBuilder sources = new StringBuilder();
        for (ScoredItem si : hits) {
            snippets.add(si.item.getContent());
            if (sources.length() > 0) sources.append("；");
            sources.append(si.item.getTitle());
        }

        // 2. 调用大模型生成（mock 或 claude）
        LlmClient client = "claude".equalsIgnoreCase(props.getProvider()) ? claudeClient : mockClient;
        String reply;
        try {
            reply = client.complete(query, snippets);
        } catch (Exception e) {
            // 模型调用失败：降级为低置信兜底，交由业务层转人工，绝不把异常抛给用户
            result.setReply("智能药师暂时无法应答，建议您转接人工药师以获得帮助。");
            result.setAiSource("");
            result.setConfidence(new BigDecimal("0.0"));
            result.setRiskLevel(RiskLevel.HIGH);
            result.setBlockedByCompliance(false);
            return result;
        }

        // 3. 置信度：以 RAG 命中强度估算（命中越相关越高；未命中则低置信）
        BigDecimal confidence = estimateConfidence(hits);

        // 4. 风险判定（C-1）
        RiskLevel risk = assessRisk(query);

        // 5. 合规过滤（C-1）：扫描模型输出是否含违规承诺
        boolean blocked = false;
        for (String bad : COMPLIANCE_BLACKLIST) {
            if (reply.contains(bad)) {
                blocked = true;
                break;
            }
        }
        if (blocked) {
            reply = "为保障用药安全与合规，该问题不便由智能助手直接答复，建议咨询执业药师。";
            confidence = new BigDecimal("0.0");
        }

        result.setReply(reply);
        result.setAiSource(sources.toString());
        result.setConfidence(confidence);
        result.setRiskLevel(risk);
        result.setBlockedByCompliance(blocked);
        return result;
    }

    /** 关键词召回打分：mock 版 RAG。统计 query 命中的关键词数作为相关度分。 */
    private List<ScoredItem> retrieve(String query, int topK) {
        List<KnowledgeItem> all = knowledgeMapper.selectAll();
        List<ScoredItem> scored = new ArrayList<>();
        for (KnowledgeItem item : all) {
            int score = 0;
            if (item.getKeywords() != null) {
                for (String kw : item.getKeywords().split(",")) {
                    kw = kw.trim();
                    if (!kw.isEmpty() && query.contains(kw)) {
                        score++;
                    }
                }
            }
            // 标题词命中也加分
            if (item.getTitle() != null) {
                for (int i = 0; i < item.getTitle().length() - 1; i++) {
                    String bigram = item.getTitle().substring(i, i + 2);
                    if (query.contains(bigram)) {
                        score++;
                        break;
                    }
                }
            }
            if (score > 0) {
                scored.add(new ScoredItem(item, score));
            }
        }
        scored.sort((a, b) -> b.score - a.score);
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

    /** 置信度估算：最高命中分映射到 0~0.95；无命中给 0.3（触发转人工）。 */
    private BigDecimal estimateConfidence(List<ScoredItem> hits) {
        if (hits.isEmpty()) {
            return new BigDecimal("0.30");
        }
        int top = hits.get(0).score;
        // 1 个命中→0.6，2 个→0.75，3+ →0.88，命中越多越自信
        double conf = Math.min(0.6 + (top - 1) * 0.14, 0.95);
        return BigDecimal.valueOf(conf).setScale(2, RoundingMode.HALF_UP);
    }

    /** 风险评估：命中高风险词即 HIGH。 */
    private RiskLevel assessRisk(String query) {
        for (String kw : HIGH_RISK_KEYWORDS) {
            if (query.contains(kw)) {
                return RiskLevel.HIGH;
            }
        }
        return RiskLevel.MEDIUM;
    }

    /** 内部打分包装。 */
    private static class ScoredItem {
        final KnowledgeItem item;
        final int score;

        ScoredItem(KnowledgeItem item, int score) {
            this.item = item;
            this.score = score;
        }
    }
}
