package com.medshop.shop.entity;

import java.math.BigDecimal;

/**
 * 订单明细实体，对应 order_item 表。unit_price 为下单时价格快照。
 */
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long drugId;
    private Integer quantity;
    private BigDecimal unitPrice;   // 下单时快照价

    // 非表字段：查询订单详情时联表带出药品名，便于前端展示
    private String drugName;

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

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }
}
