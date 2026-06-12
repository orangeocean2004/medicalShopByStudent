package com.medshop.consult.dto;

/**
 * 转人工结果响应。对应 API-07 返回参数（data 内）。
 */
public class HandoffResultDTO {

    private Long ticketId;
    private Integer status;        // 0待接 1处理中
    private Long pharmacistId;     // 分配到的药师ID（可空）

    public HandoffResultDTO(Long ticketId, Integer status, Long pharmacistId) {
        this.ticketId = ticketId;
        this.status = status;
        this.pharmacistId = pharmacistId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(Long pharmacistId) {
        this.pharmacistId = pharmacistId;
    }
}
