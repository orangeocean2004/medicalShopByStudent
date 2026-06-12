package com.medshop.consult.entity;

import java.time.LocalDateTime;

/**
 * 咨询会话实体，对应 consultation 表。
 */
public class Consultation {

    private Long id;
    private Long userId;
    private String symptom;
    private Integer transferred;   // 0否 1是
    private Long pharmacistId;
    private LocalDateTime createdAt;

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

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public Integer getTransferred() {
        return transferred;
    }

    public void setTransferred(Integer transferred) {
        this.transferred = transferred;
    }

    public Long getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(Long pharmacistId) {
        this.pharmacistId = pharmacistId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
