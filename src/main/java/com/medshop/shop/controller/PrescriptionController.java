package com.medshop.shop.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.shop.dto.PrescriptionDTO;
import com.medshop.shop.service.PrescriptionService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 处方控制层（M4 处方辅助审核）。
 *
 * <ul>
 *   <li>POST /api/v1/prescriptions            消费者上传处方（含 base64 图片）</li>
 *   <li>GET  /api/v1/prescriptions/mine       我的处方及审核状态</li>
 *   <li>GET  /api/v1/prescriptions/pending    待审处方列表（仅药师 role=1）</li>
 *   <li>PUT  /api/v1/prescriptions/{id}/review 审核通过/驳回（仅药师 role=1）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private static final int ROLE_PHARMACIST = 1;

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public Result<Long> upload(@RequestBody UploadReq req) {
        Long userId = UserContext.requireUserId();
        Long id = prescriptionService.upload(userId, req.drugId, req.imageUrl);
        return Result.success(id);
    }

    @GetMapping("/mine")
    public Result<List<PrescriptionDTO>> mine() {
        Long userId = UserContext.requireUserId();
        return Result.success(prescriptionService.listMine(userId));
    }

    @GetMapping("/pending")
    public Result<List<PrescriptionDTO>> pending() {
        UserContext.requireRole(ROLE_PHARMACIST);
        return Result.success(prescriptionService.listPending());
    }

    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody ReviewReq req) {
        UserContext.requireRole(ROLE_PHARMACIST);
        Long reviewerId = UserContext.requireUserId();
        prescriptionService.review(id, req.status, req.comment, reviewerId);
        return Result.success();
    }

    /** 上传请求：关联处方药 + base64 图片。 */
    public static class UploadReq {
        @NotNull(message = "请选择处方药")
        public Long drugId;
        public String imageUrl; // data:image/...;base64,xxxx
    }

    /** 审核请求。 */
    public static class ReviewReq {
        @NotNull(message = "审核结果不能为空")
        public Integer status;  // 1通过 2驳回
        public String comment;  // 审核备注（驳回建议填写）
    }
}
