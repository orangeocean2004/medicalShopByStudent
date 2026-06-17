package com.medshop.consult.service;

import com.medshop.common.BizException;
import com.medshop.consult.entity.ConsultationMessage;
import com.medshop.consult.entity.HandoffTicket;
import com.medshop.consult.mapper.HandoffMapper;
import com.medshop.user.entity.User;
import com.medshop.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HandoffService 单元测试（P2 人工兜底，约束 C-4 + 在线坐席判定）：
 * 有在线药师才建工单并分配、无在线坐席拒绝且不建悬空工单、客服走role=2坐席池、
 * 工单状态流转（接单记录接手人/非法状态拦截）。
 */
@ExtendWith(MockitoExtension.class)
class HandoffServiceTest {

    @Mock HandoffMapper handoffMapper;
    @Mock UserMapper userMapper;
    HandoffService service;

    @BeforeEach
    void setUp() {
        service = new HandoffService(handoffMapper, userMapper);
    }

    private User agent(long id, int role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    // ---------- 转人工建单 ----------

    @Test
    @DisplayName("有在线药师：创建咨询工单，分配给在线药师(role=1)，状态置处理中(1)")
    void createTicket_consultation_online_assigns() {
        when(userMapper.findOnlineByRole(eq(1), anyString())).thenReturn(agent(101L, 1));

        HandoffTicket t = service.createTicket(
                HandoffService.SOURCE_CONSULTATION, 55L, 1L,
                List.of(new ConsultationMessage()), "低置信转人工");

        assertEquals(101L, t.getAgentId());
        assertEquals(1, t.getStatus());
        assertEquals(HandoffService.SOURCE_CONSULTATION, t.getSourceType());
        assertEquals(55L, t.getSourceId());
        // 上下文被序列化为 JSON（C-4），非空
        assertNotNull(t.getContext());
        verify(handoffMapper).insert(t);
    }

    @Test
    @DisplayName("无在线药师：拒绝转接抛异常，不创建悬空工单")
    void createTicket_noOnlineAgent_throws() {
        when(userMapper.findOnlineByRole(eq(1), anyString())).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service.createTicket(
                HandoffService.SOURCE_CONSULTATION, 55L, 1L, List.of(), "x"));
        assertTrue(ex.getMessage().contains("在线"));
        verify(handoffMapper, never()).insert(any());
    }

    @Test
    @DisplayName("客服来源：从客服坐席池(role=2)分配，而非药师")
    void createTicket_cs_usesRole2() {
        when(userMapper.findOnlineByRole(eq(2), anyString())).thenReturn(agent(202L, 2));

        HandoffTicket t = service.createTicket(
                HandoffService.SOURCE_CS, 77L, 1L, List.of(), "客服咨询");

        assertEquals(202L, t.getAgentId());
        verify(userMapper).findOnlineByRole(eq(2), anyString());
        verify(userMapper, never()).findOnlineByRole(eq(1), anyString());
    }

    @Test
    @DisplayName("处方来源：仍走药师坐席池(role=1)")
    void createTicket_prescription_usesRole1() {
        when(userMapper.findOnlineByRole(eq(1), anyString())).thenReturn(agent(101L, 1));

        service.createTicket(HandoffService.SOURCE_PRESCRIPTION, 88L, 1L, List.of(), "处方疑问");

        verify(userMapper).findOnlineByRole(eq(1), anyString());
    }

    // ---------- 在线坐席数 ----------

    @Test
    @DisplayName("查在线坐席数：透传到 mapper 的 countOnlineByRole")
    void countOnlineAgents_delegates() {
        when(userMapper.countOnlineByRole(eq(1), anyString())).thenReturn(3);
        assertEquals(3, service.countOnlineAgents(1));
    }

    // ---------- 工单状态流转 ----------

    @Test
    @DisplayName("接单(status=1)：把工单分配给当前操作药师")
    void updateTicketStatus_take_assignsCurrentAgent() {
        HandoffTicket t = new HandoffTicket();
        t.setId(1L);
        t.setAgentId(null);
        when(handoffMapper.selectById(1L)).thenReturn(t);

        service.updateTicketStatus(1L, 1, 999L);

        verify(handoffMapper).updateStatus(1L, 1, 999L);
    }

    @Test
    @DisplayName("完成(status=2)：保留原接手药师")
    void updateTicketStatus_complete_keepsOriginalAgent() {
        HandoffTicket t = new HandoffTicket();
        t.setId(1L);
        t.setAgentId(101L);
        when(handoffMapper.selectById(1L)).thenReturn(t);

        service.updateTicketStatus(1L, 2, 999L);

        verify(handoffMapper).updateStatus(1L, 2, 101L);
    }

    @Test
    @DisplayName("更新状态：工单不存在抛异常")
    void updateTicketStatus_notFound_throws() {
        when(handoffMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> service.updateTicketStatus(1L, 1, 999L));
        verify(handoffMapper, never()).updateStatus(anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("更新状态：非法状态值(>2)抛异常")
    void updateTicketStatus_illegal_throws() {
        HandoffTicket t = new HandoffTicket();
        t.setId(1L);
        when(handoffMapper.selectById(1L)).thenReturn(t);

        assertThrows(BizException.class, () -> service.updateTicketStatus(1L, 3, 999L));
        verify(handoffMapper, never()).updateStatus(anyLong(), anyInt(), anyLong());
    }
}
