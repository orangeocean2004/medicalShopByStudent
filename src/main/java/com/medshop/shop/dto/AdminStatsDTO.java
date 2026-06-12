package com.medshop.shop.dto;

import java.math.BigDecimal;

/**
 * 运营管理后台「网站统计」聚合数据。
 */
public class AdminStatsDTO {

    // 用户（按角色）
    private int userConsumers;   // 消费者
    private int userPharmacists; // 执业药师
    private int userStaff;       // 客服 + 运营
    private int userTotal;

    // 订单（按状态）
    private int orderPending;    // 待支付
    private int orderPaid;       // 已支付
    private int orderShipping;   // 配送中
    private int orderDone;       // 已完成
    private int orderCancelled;  // 已取消
    private int orderTotal;

    // 营收：已支付及以后（不含待支付/已取消）
    private BigDecimal revenue = BigDecimal.ZERO;

    // 药品
    private int drugTotal;
    private int drugLowStock;    // 库存偏低（<=60）

    public int getUserConsumers() { return userConsumers; }
    public void setUserConsumers(int v) { this.userConsumers = v; }

    public int getUserPharmacists() { return userPharmacists; }
    public void setUserPharmacists(int v) { this.userPharmacists = v; }

    public int getUserStaff() { return userStaff; }
    public void setUserStaff(int v) { this.userStaff = v; }

    public int getUserTotal() { return userTotal; }
    public void setUserTotal(int v) { this.userTotal = v; }

    public int getOrderPending() { return orderPending; }
    public void setOrderPending(int v) { this.orderPending = v; }

    public int getOrderPaid() { return orderPaid; }
    public void setOrderPaid(int v) { this.orderPaid = v; }

    public int getOrderShipping() { return orderShipping; }
    public void setOrderShipping(int v) { this.orderShipping = v; }

    public int getOrderDone() { return orderDone; }
    public void setOrderDone(int v) { this.orderDone = v; }

    public int getOrderCancelled() { return orderCancelled; }
    public void setOrderCancelled(int v) { this.orderCancelled = v; }

    public int getOrderTotal() { return orderTotal; }
    public void setOrderTotal(int v) { this.orderTotal = v; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal v) { this.revenue = v; }

    public int getDrugTotal() { return drugTotal; }
    public void setDrugTotal(int v) { this.drugTotal = v; }

    public int getDrugLowStock() { return drugLowStock; }
    public void setDrugLowStock(int v) { this.drugLowStock = v; }
}
