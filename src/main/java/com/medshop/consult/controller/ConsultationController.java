package com.medshop.consult.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.consult.dto.AiReplyDTO;
import com.medshop.consult.dto.HandoffResultDTO;
import com.medshop.consult.dto.MessageDTO;
import com.medshop.consult.dto.MessageItemDTO;
import com.medshop.consult.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 智能药师咨询控制层（M2）。
 * <ul>
 *   <li>创建会话：POST /api/v1/consultations（便于演示，返回 consultationId）</li>
 *   <li>API-06 发起 AI 咨询：POST /api/v1/consultations/{id}/messages</li>
 *   <li>API-07 转接人工药师：POST /api/v1/consultations/{id}/handoff</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    /** 创建咨询会话，body 可选携带 symptom。 */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody(required = false) Map<String, String> body) {
        Long userId = UserContext.requireUserId();
        String symptom = body != null ? body.getOrDefault("symptom", null) : null;
        Long id = consultationService.createConsultation(userId, symptom);
        return Result.success(Collections.singletonMap("consultationId", id));
    }

    /** API-06：在会话中发起 AI 用药咨询。 */
    @PostMapping("/{id}/messages")
    public Result<AiReplyDTO> sendMessage(@PathVariable Long id, @Valid @RequestBody MessageDTO dto) {
        Long userId = UserContext.requireUserId();
        return Result.success(consultationService.askAi(userId, id, dto.getContent()));
    }

    /** API-07：转接人工药师。 */
    @PostMapping("/{id}/handoff")
    public Result<HandoffResultDTO> handoff(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body) {
        Long userId = UserContext.requireUserId();
        String reason = body != null ? body.get("reason") : null;
        return Result.success(consultationService.handoffToPharmacist(userId, id, reason));
    }

    /** 拉取会话消息（用户与接手药师轮询共用）。 */
    @GetMapping("/{id}/messages")
    public Result<List<MessageItemDTO>> messages(@PathVariable Long id) {
        Long requesterId = UserContext.requireUserId();
        Integer role = UserContext.getRole();
        return Result.success(consultationService.getMessages(requesterId, role, id));
    }

    /** 药师在已转人工的会话中回复用户（role=1）。 */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody MessageDTO dto) {
        UserContext.requireRole(1);
        Long pharmacistId = UserContext.requireUserId();
        consultationService.replyByPharmacist(pharmacistId, id, dto.getContent());
        return Result.success();
    }

    /** 用户结束人工咨询，切回智能药师。 */
    @PostMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        consultationService.resumeAi(userId, id);
        return Result.success();
    }
}
