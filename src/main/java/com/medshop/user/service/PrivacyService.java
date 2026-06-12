package com.medshop.user.service;

import com.medshop.common.BizException;
import com.medshop.user.dto.PrivacyAuthDTO;
import com.medshop.user.entity.PrivacyAuthorization;
import com.medshop.user.mapper.PrivacyMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 隐私授权业务层（C-3）：开启 / 撤回个性化推荐、健康档案等授权，支持撤回（最小必要原则）。
 */
@Service
public class PrivacyService {

    private final PrivacyMapper privacyMapper;

    public PrivacyService(PrivacyMapper privacyMapper) {
        this.privacyMapper = privacyMapper;
    }

    /**
     * 设置授权状态：不存在则新建，存在则更新；撤回（status=0）时记录撤回时间。
     */
    public PrivacyAuthorization setAuthorization(Long userId, PrivacyAuthDTO dto) {
        if (dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BizException("授权状态只能为 0（撤回）或 1（授权）");
        }
        PrivacyAuthorization existing = privacyMapper.findByUserAndScope(userId, dto.getScope());
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            PrivacyAuthorization auth = new PrivacyAuthorization();
            auth.setUserId(userId);
            auth.setScope(dto.getScope());
            auth.setStatus(dto.getStatus());
            auth.setAuthorizedAt(now);
            auth.setRevokedAt(dto.getStatus() == 0 ? now : null);
            privacyMapper.insert(auth);
            return auth;
        } else {
            existing.setStatus(dto.getStatus());
            if (dto.getStatus() == 0) {
                existing.setRevokedAt(now);          // 撤回
            } else {
                existing.setAuthorizedAt(now);       // 重新授权
                existing.setRevokedAt(null);
            }
            privacyMapper.updateStatus(existing);
            return existing;
        }
    }
}
