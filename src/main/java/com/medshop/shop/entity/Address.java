package com.medshop.shop.entity;

import java.time.LocalDateTime;

/**
 * 收货地址实体，对应 user_address 表。
 *
 * <p>消费者可维护多个收货地址，下单时选择其一；{@link #isDefault} 标记默认地址，
 * 结算页默认选中。同一用户至多一个默认地址（设默认时先清除其余默认）。
 */
public class Address {

    private Long id;
    private Long userId;
    private String receiver;       // 收货人
    private String phone;          // 联系电话
    private String region;         // 省市区
    private String detail;         // 详细地址
    private Integer isDefault;     // 0否 1是
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
