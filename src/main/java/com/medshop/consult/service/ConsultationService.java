package com.medshop.consult.service;

import com.medshop.common.BizException;
import com.medshop.consult.ai.AiGatewayService;
import com.medshop.consult.ai.AiProperties;
import com.medshop.consult.ai.AiResult;
import com.medshop.consult.ai.RiskLevel;
import com.medshop.consult.dto.AiReplyDTO;
import com.medshop.consult.dto.HandoffResultDTO;
import com.medshop.consult.dto.MessageItemDTO;
import com.medshop.consult.entity.Consultation;
import com.medshop.consult.entity.ConsultationMessage;
import com.medshop.consult.entity.HandoffTicket;
import com.medshop.consult.mapper.ConsultationMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能药师咨询业务层（M2，核心类）。
 *
 * <p>编排一次 AI 咨询的完整流程：存用户消息 → 调 AI 中台 P1 生成建议 →
 * 合规/置信度判定 → 决定是否触发人工兜底 P2 → 存 AI 消息。
 *
 * <p>体现系统特色：业务层不直接调用大模型，而是通过注入
 * {@link AiGatewayService}（P1）与 {@link HandoffService}（P2）复用公共能力，
 * 把 AI 不确定性与合规风险隔离在公共层。
 */
@Service
public class ConsultationService {

    private static final int SENDER_USER = 0;
    private static final int SENDER_AI = 1;
    private static final int SENDER_PHARMACIST = 2;
    private static final int ROLE_PHARMACIST = 1;
    private static final java.time.format.DateTimeFormatter TIME_FMT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    private final ConsultationMapper consultationMapper;
    private final AiGatewayService aiGatewayService;   // P1
    private final HandoffService handoffService;       // P2
    private final AiProperties aiProperties;

    public ConsultationService(ConsultationMapper consultationMapper,
                               AiGatewayService aiGatewayService,
                               HandoffService handoffService,
                               AiProperties aiProperties) {
        this.consultationMapper = consultationMapper;
        this.aiGatewayService = aiGatewayService;
        this.handoffService = handoffService;
        this.aiProperties = aiProperties;
    }

    /**
     * 新建咨询会话。
     */
    public Long createConsultation(Long userId, String symptom) {
        Consultation c = new Consultation();
        c.setUserId(userId);
        c.setSymptom(symptom);
        c.setTransferred(0);
        consultationMapper.insertConsultation(c);
        return c.getId();
    }

    /**
     * 编排 AI 咨询全流程（FP-AI-01/02/03/05）。
     */
    public AiReplyDTO askAi(Long userId, Long consultationId, String content) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BizException("咨询会话不存在或无权访问");
        }

        // 0. 已转人工：会话进入人工模式，AI 退出，仅存用户消息，等药师回复（轮询拉取）
        if (c.getTransferred() != null && c.getTransferred() == 1) {
            saveMessage(consultationId, SENDER_USER, content, null, null);
            AiReplyDTO dto = new AiReplyDTO();
            dto.setReply(null);          // 人工模式无 AI 回复
            dto.setHandoffMode(true);    // 前端据此切换为「等待药师」展示
            dto.setNeedHandoff(false);
            return dto;
        }

        // 1. 存用户消息
        saveMessage(consultationId, SENDER_USER, content, null, null);

        // 2. 调用 AI 中台 P1 生成建议（业务层不直接碰大模型）
        AiResult ai = aiGatewayService.generate(content);

        // 3. 是否需转人工：低置信 / 高风险 / 命中合规拦截（C-1）
        boolean lowConfidence = ai.getConfidence().compareTo(aiProperties.getConfidenceThreshold()) < 0;
        boolean needHandoff = lowConfidence
                || ai.getRiskLevel() == RiskLevel.HIGH
                || ai.isBlockedByCompliance();

        // 4. 存 AI 消息（FP-AI-05 留存来源与置信度，可追溯）
        saveMessage(consultationId, SENDER_AI, ai.getReply(), ai.getAiSource(), ai.getConfidence());

        // 5. 组装返回，前端据 needHandoff 决定是否展示「一键转人工」
        AiReplyDTO dto = new AiReplyDTO();
        dto.setReply(ai.getReply());
        dto.setAiSource(ai.getAiSource());
        dto.setConfidence(ai.getConfidence());
        dto.setNeedHandoff(needHandoff);
        return dto;
    }

    /**
     * 转人工药师（FP-AI-04，经公共模块 P2）。
     */
    public HandoffResultDTO handoffToPharmacist(Long userId, Long consultationId, String reason) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BizException("咨询会话不存在或无权访问");
        }

        // 1. 取完整对话上下文（C-4）
        List<ConsultationMessage> context = consultationMapper.selectMessages(consultationId);

        // 2. 调用公共模块 P2 创建兜底工单（携带上下文）
        HandoffTicket ticket = handoffService.createTicket(
                HandoffService.SOURCE_CONSULTATION, consultationId, userId, context,
                reason != null ? reason : "用户请求人工确认");

        // 3. 更新会话状态为已转人工，记录接手药师
        consultationMapper.markTransferred(consultationId, ticket.getAgentId());

        return new HandoffResultDTO(ticket.getId(), ticket.getStatus(), ticket.getAgentId());
    }

    /**
     * 拉取会话全部消息（用户端与药师端轮询共用）。
     * 用户只能看自己的会话；药师须为该会话的接手人。
     */
    public List<MessageItemDTO> getMessages(Long requesterId, Integer requesterRole, Long consultationId) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null) {
            throw new BizException("咨询会话不存在");
        }
        boolean isOwner = c.getUserId().equals(requesterId);
        boolean isAgent = requesterRole != null && requesterRole == ROLE_PHARMACIST
                && c.getPharmacistId() != null && c.getPharmacistId().equals(requesterId);
        if (!isOwner && !isAgent) {
            throw new BizException("无权查看该会话");
        }
        List<ConsultationMessage> msgs = consultationMapper.selectMessages(consultationId);
        List<MessageItemDTO> list = new ArrayList<>();
        for (ConsultationMessage m : msgs) {
            MessageItemDTO d = new MessageItemDTO();
            d.setId(m.getId());
            d.setSenderType(m.getSenderType());
            d.setContent(m.getContent());
            d.setAiSource(m.getAiSource());
            d.setConfidence(m.getConfidence());
            d.setCreatedAt(m.getCreatedAt() == null ? null : m.getCreatedAt().format(TIME_FMT));
            list.add(d);
        }
        return list;
    }

    /**
     * 药师在已转人工的会话中回复用户（FP-AI-04 人工接管）。
     * 校验：调用者须为该会话的接手药师；会话须已转人工。
     */
    public void replyByPharmacist(Long pharmacistId, Long consultationId, String content) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null) {
            throw new BizException("咨询会话不存在");
        }
        if (c.getTransferred() == null || c.getTransferred() != 1) {
            throw new BizException("该会话尚未转人工");
        }
        if (c.getPharmacistId() == null || !c.getPharmacistId().equals(pharmacistId)) {
            throw new BizException("你不是该会话的接手药师");
        }
        saveMessage(consultationId, SENDER_PHARMACIST, content, null, null);
    }

    /**
     * 结束人工咨询，切回智能药师（用户主动操作）。
     * 仅本人可操作；幂等——未处于人工模式直接返回。
     */
    public void resumeAi(Long userId, Long consultationId) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BizException("咨询会话不存在或无权访问");
        }
        if (c.getTransferred() == null || c.getTransferred() != 1) {
            return; // 已是 AI 模式，幂等
        }
        consultationMapper.markResumed(consultationId);
    }

    private void saveMessage(Long consultationId, int senderType, String content,
                             String aiSource, java.math.BigDecimal confidence) {
        ConsultationMessage msg = new ConsultationMessage();
        msg.setConsultationId(consultationId);
        msg.setSenderType(senderType);
        msg.setContent(content);
        msg.setAiSource(aiSource);
        msg.setConfidence(confidence);
        consultationMapper.insertMessage(msg);
    }
}
