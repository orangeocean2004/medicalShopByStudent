package com.medshop.user.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.user.dto.PrivacyAuthDTO;
import com.medshop.user.entity.PrivacyAuthorization;
import com.medshop.user.service.PrivacyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 隐私授权控制层（P3，FP-REC-04，约束 C-3）。
 * API-10 隐私授权管理：PUT /api/v1/privacy/authorization
 */
@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @PutMapping("/authorization")
    public Result<Map<String, Object>> setAuthorization(@Valid @RequestBody PrivacyAuthDTO dto) {
        Long userId = UserContext.requireUserId();
        PrivacyAuthorization auth = privacyService.setAuthorization(userId, dto);

        // 按 API-10 返回 scope/status/updatedAt
        LocalDateTime updatedAt = auth.getStatus() == 0 ? auth.getRevokedAt() : auth.getAuthorizedAt();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", auth.getScope());
        data.put("status", auth.getStatus());
        data.put("updatedAt", updatedAt != null ? updatedAt.format(FMT) : null);
        return Result.success(data);
    }
}
