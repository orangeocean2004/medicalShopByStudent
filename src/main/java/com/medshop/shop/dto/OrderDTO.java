package com.medshop.shop.dto;

import com.medshop.shop.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单传输对象。对应 API-04 返回（创建）与 API-05 返回（详情含明细与物流）。
 */
public class OrderDTO {

    private Long orderId;
    private Long userId;           // 运营后台列表展示下单用户
    private BigDecimal totalAmount;
    private Integer status;        // 0待支付 1已支付 2配送中 3已完成 4已取消
    private String logisticsNo;
    private String createdAt;      // 下单时间（列表展示）
    private List<OrderItem> items; // 查询详情时返回明细

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLogisticsNo() {
        return logisticsNo;
    }

    public void setLogisticsNo(String logisticsNo) {
        this.logisticsNo = logisticsNo;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
