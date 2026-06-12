package com.medshop.consult.service;

import com.medshop.consult.dto.WorkbenchStatsDTO;
import com.medshop.consult.mapper.HandoffMapper;
import com.medshop.shop.mapper.PrescriptionMapper;
import org.springframework.stereotype.Service;

/**
 * 药师工作台统计：按当前药师维度聚合工单与处方审核工作量。
 */
@Service
public class WorkbenchStatsService {

    private final HandoffMapper handoffMapper;
    private final PrescriptionMapper prescriptionMapper;

    public WorkbenchStatsService(HandoffMapper handoffMapper, PrescriptionMapper prescriptionMapper) {
        this.handoffMapper = handoffMapper;
        this.prescriptionMapper = prescriptionMapper;
    }

    public WorkbenchStatsDTO statsForPharmacist(Long pharmacistId) {
        WorkbenchStatsDTO s = new WorkbenchStatsDTO();

        // 本药师接手的工单
        int doing = handoffMapper.countByAgent(pharmacistId, 1);
        int done = handoffMapper.countByAgent(pharmacistId, 2);
        s.setTicketDoing(doing);
        s.setTicketDone(done);
        s.setTicketTotal(handoffMapper.countByAgent(pharmacistId, null));

        // 本药师审核的处方
        int approved = prescriptionMapper.countByReviewer(pharmacistId, 1);
        int rejected = prescriptionMapper.countByReviewer(pharmacistId, 2);
        s.setRxApproved(approved);
        s.setRxRejected(rejected);
        s.setRxTotal(prescriptionMapper.countByReviewer(pharmacistId, null));

        // 全局待处理（提示用）
        s.setPendingTickets(handoffMapper.selectAll().stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 0).toList().size());
        s.setPendingRx(prescriptionMapper.selectPending().size());

        return s;
    }
}
