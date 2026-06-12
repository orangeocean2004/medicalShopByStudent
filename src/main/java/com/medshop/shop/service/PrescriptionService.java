package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.shop.dto.PrescriptionDTO;
import com.medshop.shop.entity.Drug;
import com.medshop.shop.entity.Prescription;
import com.medshop.shop.mapper.DrugMapper;
import com.medshop.shop.mapper.PrescriptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 处方业务层（M4 处方辅助审核）。
 *
 * <p>消费者上传处方药的处方图片 → 生成待审处方 → 执业药师审核通过/驳回。
 * 仅当某处方药存在「审核通过」的处方时，{@link OrderService} 才放行下单。
 */
@Service
public class PrescriptionService {

    /** 单张处方图片大小上限（base64 字符串长度），约 2MB 原图。 */
    private static final int MAX_IMAGE_LEN = 3_000_000;

    private final PrescriptionMapper prescriptionMapper;
    private final DrugMapper drugMapper;

    public PrescriptionService(PrescriptionMapper prescriptionMapper, DrugMapper drugMapper) {
        this.prescriptionMapper = prescriptionMapper;
        this.drugMapper = drugMapper;
    }

    /**
     * 上传处方（FP-RX-01）。校验药品存在且确为处方药，存图片与待审记录。
     */
    public Long upload(Long userId, Long drugId, String imageUrl) {
        if (drugId == null) {
            throw new BizException("请选择要购买的处方药");
        }
        if (!StringUtils.hasText(imageUrl)) {
            throw new BizException("请上传处方图片");
        }
        if (imageUrl.length() > MAX_IMAGE_LEN) {
            throw new BizException("处方图片过大，请压缩后重试");
        }
        Drug drug = drugMapper.selectById(drugId);
        if (drug == null) {
            throw new BizException("药品不存在");
        }
        if (drug.getIsRx() == null || drug.getIsRx() != 1) {
            throw new BizException("「" + drug.getName() + "」非处方药，无需上传处方");
        }

        Prescription p = new Prescription();
        p.setUserId(userId);
        p.setDrugId(drugId);
        p.setImageUrl(imageUrl);
        p.setStatus(0); // 待审
        prescriptionMapper.insert(p);
        return p.getId();
    }

    /** 我的处方列表。 */
    public List<PrescriptionDTO> listMine(Long userId) {
        return prescriptionMapper.selectByUser(userId);
    }

    /** 待审处方列表（药师工作台）。 */
    public List<PrescriptionDTO> listPending() {
        return prescriptionMapper.selectPending();
    }

    /**
     * 药师审核（FP-RX-02）。status：1通过 / 2驳回；驳回建议填写备注。
     */
    public void review(Long prescriptionId, Integer status, String comment, Long reviewerId) {
        Prescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null) {
            throw new BizException("处方不存在");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BizException("审核结果非法（1通过 / 2驳回）");
        }
        if (p.getStatus() != null && p.getStatus() != 0) {
            throw new BizException("该处方已审核，请勿重复操作");
        }
        prescriptionMapper.updateReview(prescriptionId, status, comment, reviewerId);
    }

    /** 该用户对该处方药是否已有审核通过的处方（下单放行判定）。 */
    public boolean hasApproved(Long userId, Long drugId) {
        return prescriptionMapper.countApprovedByUserAndDrug(userId, drugId) > 0;
    }
}
