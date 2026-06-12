package com.medshop.consult.dto;

/**
 * 药师工作台工作量统计（个人维度）。
 */
public class WorkbenchStatsDTO {

    // 转人工工单（本药师接手的）
    private int ticketDoing;     // 处理中
    private int ticketDone;      // 已完成
    private int ticketTotal;     // 合计

    // 处方审核（本药师审核的）
    private int rxApproved;      // 通过
    private int rxRejected;      // 驳回
    private int rxTotal;         // 合计

    // 当前全局待处理（提示用，非个人维度）
    private int pendingTickets;  // 待接单工单数
    private int pendingRx;       // 待审处方数

    public int getTicketDoing() {
        return ticketDoing;
    }

    public void setTicketDoing(int ticketDoing) {
        this.ticketDoing = ticketDoing;
    }

    public int getTicketDone() {
        return ticketDone;
    }

    public void setTicketDone(int ticketDone) {
        this.ticketDone = ticketDone;
    }

    public int getTicketTotal() {
        return ticketTotal;
    }

    public void setTicketTotal(int ticketTotal) {
        this.ticketTotal = ticketTotal;
    }

    public int getRxApproved() {
        return rxApproved;
    }

    public void setRxApproved(int rxApproved) {
        this.rxApproved = rxApproved;
    }

    public int getRxRejected() {
        return rxRejected;
    }

    public void setRxRejected(int rxRejected) {
        this.rxRejected = rxRejected;
    }

    public int getRxTotal() {
        return rxTotal;
    }

    public void setRxTotal(int rxTotal) {
        this.rxTotal = rxTotal;
    }

    public int getPendingTickets() {
        return pendingTickets;
    }

    public void setPendingTickets(int pendingTickets) {
        this.pendingTickets = pendingTickets;
    }

    public int getPendingRx() {
        return pendingRx;
    }

    public void setPendingRx(int pendingRx) {
        this.pendingRx = pendingRx;
    }
}
