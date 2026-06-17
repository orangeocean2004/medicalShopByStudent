package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.shop.entity.Drug;
import com.medshop.shop.entity.Prescription;
import com.medshop.shop.mapper.DrugMapper;
import com.medshop.shop.mapper.PrescriptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PrescriptionService 单元测试（M4 处方辅助审核）：
 * 上传校验（缺图/超大图/非处方药/药品不存在）、审核（重复/非法状态）、下单放行判定。
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock PrescriptionMapper prescriptionMapper;
    @Mock DrugMapper drugMapper;
    PrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptionMapper, drugMapper);
    }

    private Drug rxDrug() {
        Drug d = new Drug();
        d.setId(5004L);
        d.setName("阿莫西林");
        d.setIsRx(1);
        return d;
    }

    private Drug otcDrug() {
        Drug d = new Drug();
        d.setId(5001L);
        d.setName("感冒灵");
        d.setIsRx(0);
        return d;
    }

    // ---------- 上传 ----------

    @Test
    @DisplayName("上传成功：处方药+合法图片，落待审(status=0)记录并回填id")
    void upload_success() {
        when(drugMapper.selectById(5004L)).thenReturn(rxDrug());
        doAnswer(inv -> {
            Prescription p = inv.getArgument(0);
            p.setId(777L);
            return null;
        }).when(prescriptionMapper).insert(any());

        Long id = service.upload(1L, 5004L, "data:image/png;base64,AAAA");

        assertEquals(777L, id);
        ArgumentCaptor<Prescription> cap = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionMapper).insert(cap.capture());
        Prescription saved = cap.getValue();
        assertEquals(0, saved.getStatus());
        assertEquals(1L, saved.getUserId());
        assertEquals(5004L, saved.getDrugId());
    }

    @Test
    @DisplayName("上传：drugId为空抛异常")
    void upload_nullDrugId_throws() {
        assertThrows(BizException.class, () -> service.upload(1L, null, "data:img"));
        verify(prescriptionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传：未提供图片抛异常")
    void upload_blankImage_throws() {
        assertThrows(BizException.class, () -> service.upload(1L, 5004L, "  "));
        assertThrows(BizException.class, () -> service.upload(1L, 5004L, null));
        verify(prescriptionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传：图片超过大小上限抛异常")
    void upload_oversizeImage_throws() {
        String huge = "x".repeat(3_000_001);
        assertThrows(BizException.class, () -> service.upload(1L, 5004L, huge));
        verify(prescriptionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传：药品不存在抛异常")
    void upload_drugNotFound_throws() {
        when(drugMapper.selectById(9999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.upload(1L, 9999L, "data:img"));
        verify(prescriptionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("上传：非处方药无需上传处方，抛异常")
    void upload_otcDrug_throws() {
        when(drugMapper.selectById(5001L)).thenReturn(otcDrug());
        BizException ex = assertThrows(BizException.class,
                () -> service.upload(1L, 5001L, "data:img"));
        assertTrue(ex.getMessage().contains("非处方药"));
        verify(prescriptionMapper, never()).insert(any());
    }

    // ---------- 审核 ----------

    @Test
    @DisplayName("审核通过：待审处方置为通过(1)")
    void review_approve_success() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setStatus(0);
        when(prescriptionMapper.selectById(1L)).thenReturn(p);

        service.review(1L, 1, "处方有效", 100L);

        verify(prescriptionMapper).updateReview(1L, 1, "处方有效", 100L);
    }

    @Test
    @DisplayName("审核驳回：待审处方置为驳回(2)带备注")
    void review_reject_success() {
        Prescription p = new Prescription();
        p.setStatus(0);
        when(prescriptionMapper.selectById(1L)).thenReturn(p);

        service.review(1L, 2, "字迹不清", 100L);

        verify(prescriptionMapper).updateReview(1L, 2, "字迹不清", 100L);
    }

    @Test
    @DisplayName("审核：处方不存在抛异常")
    void review_notFound_throws() {
        when(prescriptionMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> service.review(1L, 1, null, 100L));
        verify(prescriptionMapper, never()).updateReview(anyLong(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("审核：非法结果状态（非1/2，如null/3）抛异常")
    void review_illegalStatus_throws() {
        Prescription p = new Prescription();
        p.setStatus(0);
        when(prescriptionMapper.selectById(1L)).thenReturn(p);

        assertThrows(BizException.class, () -> service.review(1L, null, null, 100L));
        assertThrows(BizException.class, () -> service.review(1L, 3, null, 100L));
        verify(prescriptionMapper, never()).updateReview(anyLong(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("审核：已审核过的处方不可重复操作")
    void review_alreadyReviewed_throws() {
        Prescription p = new Prescription();
        p.setStatus(1); // 已通过
        when(prescriptionMapper.selectById(1L)).thenReturn(p);

        BizException ex = assertThrows(BizException.class, () -> service.review(1L, 2, null, 100L));
        assertTrue(ex.getMessage().contains("重复"));
        verify(prescriptionMapper, never()).updateReview(anyLong(), any(), any(), anyLong());
    }

    // ---------- 下单放行判定 ----------

    @Test
    @DisplayName("放行判定：存在审核通过的处方时返回true")
    void hasApproved_true() {
        when(prescriptionMapper.countApprovedByUserAndDrug(1L, 5004L)).thenReturn(1);
        assertTrue(service.hasApproved(1L, 5004L));
    }

    @Test
    @DisplayName("放行判定：无审核通过处方返回false")
    void hasApproved_false() {
        when(prescriptionMapper.countApprovedByUserAndDrug(1L, 5004L)).thenReturn(0);
        assertFalse(service.hasApproved(1L, 5004L));
    }
}
