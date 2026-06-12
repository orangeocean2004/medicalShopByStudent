package com.medshop.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体，对应 payment 表。下单时生成"待支付"记录，与订单解耦。
 */
public class Payment {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private Integer channel;       // 1微信 2支付宝 3银行卡
    private Integer status;        // 0待支付 1成功 2失败
    private LocalDateTime paidAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
