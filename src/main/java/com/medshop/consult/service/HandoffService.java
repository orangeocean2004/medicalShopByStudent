package com.medshop.consult.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medshop.common.BizException;
import com.medshop.consult.entity.ConsultationMessage;
import com.medshop.consult.entity.HandoffTicket;
import com.medshop.consult.mapper.HandoffMapper;
import com.medshop.user.entity.User;
import com.medshop.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 人工兜底服务（公共模块 P2，约束 C-4）。
 *
 * <p>统一处理所有来源（咨询/客服/处方）的转人工：打包上下文、创建工单、分配坐席。
 * M2 咨询、M5 客服等模块复用本服务，保证兜底口径一致。
 *
 * <p>坐席在线判定：用户每次带 Token 请求都会刷新 last_active_at（见 AuthInterceptor），
 * 最近 {@link #ONLINE_TIMEOUT_SECONDS} 秒内活跃过的坐席视为在线。转人工时若无在线坐席，
 * 直接拒绝并提示用户稍后再试，不创建悬空工单。
 */
@Service
public class HandoffService {

    /** 工单来源类型。 */
    public static final int SOURCE_CONSULTATION = 1; // 药师咨询
    public static final int SOURCE_CS = 2;           // 客服
    public static final int SOURCE_PRESCRIPTION = 3; // 处方

    /** 坐席在线超时（秒）：超过此时长未活动即判离线。略大于前端轮询间隔以容忍抖动。 */
    public static final long ONLINE_TIMEOUT_SECONDS = 90;

    private final HandoffMapper handoffMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HandoffService(HandoffMapper handoffMapper, UserMapper userMapper) {
        this.handoffMapper = handoffMapper;
        this.userMapper = userMapper;
    }

    /** SQLite CURRENT_TIMESTAMP 的存储格式（UTC，空格分隔），用于时间字符串比较对齐。 */
    private static final DateTimeFormatter SQLITE_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 在线判定的时间下界字符串。
     * SQLite 以 'yyyy-MM-dd HH:mm:ss'（UTC）存储 CURRENT_TIMESTAMP，
     * 这里必须产出同格式同时区的字符串，否则字符串比较会因 ISO 的 'T' 分隔符而错乱。
     */
    private String onlineSince() {
        return LocalDateTime.now(ZoneOffset.UTC)
                .minusSeconds(ONLINE_TIMEOUT_SECONDS)
                .format(SQLITE_TS);
    }

    /** 当前在线坐席数（按角色）。咨询/处方看药师(role=1)，客服看客服(role=2)。 */
    public int countOnlineAgents(int agentRole) {
        return userMapper.countOnlineByRole(agentRole, onlineSince());
    }

    /**
     * 创建兜底工单并分配坐席。
     *
     * @param sourceType 来源类型（见常量）
     * @param sourceId   来源会话/处方ID
     * @param userId     发起用户
     * @param context    携带的对话上下文（C-4 完整留存）
     * @param reason     触发原因
     * @return 工单（含分配到的坐席ID）
     * @throws BizException 当前无在线坐席时拒绝转接
     */
    public HandoffTicket createTicket(int sourceType, Long sourceId, Long userId,
                                      List<ConsultationMessage> context, String reason) {
        // 1. 序列化上下文为 JSON（C-4：完整对话随工单流转）
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            contextJson = "[]";
        }

        // 2. 按来源选择坐席池：咨询→执业药师(role=1)，客服→客服(role=2)
        int agentRole = (sourceType == SOURCE_CS) ? 2 : 1;
        // 仅认「在线」坐席：取最近活跃的一个；无在线坐席则拒绝转接
        User agent = userMapper.findOnlineByRole(agentRole, onlineSince());
        if (agent == null) {
            throw new BizException("当前没有在线的人工药师，请稍后再试，或留言等待药师上线后处理。");
        }

        // 3. 落工单：分配给在线坐席并置处理中
        HandoffTicket ticket = new HandoffTicket();
        ticket.setSourceType(sourceType);
        ticket.setSourceId(sourceId);
        ticket.setUserId(userId);
        ticket.setContext(contextJson);
        ticket.setReason(reason);
        ticket.setAgentId(agent.getId());
        ticket.setStatus(1); // 处理中（已分配在线药师）
        handoffMapper.insert(ticket);
        return ticket;
    }

    /** 全部工单列表（药师工作台）。 */
    public List<com.medshop.consult.dto.HandoffTicketDTO> listTickets() {
        return handoffMapper.selectAll();
    }

    /**
     * 药师更新工单状态：接单（1处理中）或完成（2已完成）。
     * 接单时把工单分配给当前操作的药师。
     */
    public void updateTicketStatus(Long ticketId, Integer status, Long agentId) {
        HandoffTicket ticket = handoffMapper.selectById(ticketId);
        if (ticket == null) {
            throw new com.medshop.common.BizException("工单不存在");
        }
        if (status < 0 || status > 2) {
            throw new com.medshop.common.BizException("非法的工单状态");
        }
        // 接单/处理中：记录接手药师；完成：保留原 agent 或记当前药师
        Long assignee = (ticket.getAgentId() != null) ? ticket.getAgentId() : agentId;
        if (status == 1) {
            assignee = agentId;
        }
        handoffMapper.updateStatus(ticketId, status, assignee);
    }
}
