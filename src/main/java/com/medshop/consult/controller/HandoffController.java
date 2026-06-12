package com.medshop.consult.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.consult.dto.HandoffTicketDTO;
import com.medshop.consult.dto.WorkbenchStatsDTO;
import com.medshop.consult.service.HandoffService;
import com.medshop.consult.service.WorkbenchStatsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药师工作台控制层（角色 1 执业药师）。
 * 药师在此查看由智能药师转来的人工兜底工单（C-4 携带完整对话上下文），
 * 并接单 / 标记完成。越权由 {@link UserContext#requireRole} 拦截返回 403。
 *
 * <ul>
 *   <li>GET /api/v1/handoff/tickets        工单列表（仅药师）</li>
 *   <li>PUT /api/v1/handoff/tickets/{id}   接单/完成（仅药师）</li>
 *   <li>GET /api/v1/handoff/online         在线药师状态（任意登录用户，供消费者/运营轮询展示）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/handoff")
public class HandoffController {

    private static final int ROLE_PHARMACIST = 1;

    private final HandoffService handoffService;
    private final WorkbenchStatsService statsService;

    public HandoffController(HandoffService handoffService, WorkbenchStatsService statsService) {
        this.handoffService = handoffService;
        this.statsService = statsService;
    }

    /** 药师工作台个人工作量统计（仅药师）。 */
    @GetMapping("/stats")
    public Result<WorkbenchStatsDTO> stats() {
        UserContext.requireRole(ROLE_PHARMACIST);
        Long pharmacistId = UserContext.requireUserId();
        return Result.success(statsService.statsForPharmacist(pharmacistId));
    }

    @GetMapping("/tickets")
    public Result<List<HandoffTicketDTO>> listTickets() {
        UserContext.requireRole(ROLE_PHARMACIST);
        return Result.success(handoffService.listTickets());
    }

    @PutMapping("/tickets/{id}")
    public Result<Void> updateTicket(@PathVariable Long id, @RequestBody @Valid StatusReq req) {
        UserContext.requireRole(ROLE_PHARMACIST);
        Long agentId = UserContext.requireUserId();
        handoffService.updateTicketStatus(id, req.status, agentId);
        return Result.success();
    }

    /**
     * 在线药师状态：返回在线药师数与是否有药师在线。
     * 任意登录用户可访问，供消费者咨询页 / 运营后台轮询显示「药师在线/离线」。
     */
    @GetMapping("/online")
    public Result<Map<String, Object>> onlineStatus() {
        UserContext.requireUserId();
        int count = handoffService.countOnlineAgents(ROLE_PHARMACIST);
        Map<String, Object> data = new HashMap<>();
        data.put("online", count > 0);
        data.put("count", count);
        return Result.success(data);
    }

    public static class StatusReq {
        @NotNull(message = "状态不能为空")
        public Integer status; // 1处理中(接单) 2已完成
    }
}
