package com.medshop.shop.entity;

import java.time.LocalDateTime;

/**
 * 处方实体，对应 prescription 表（M4 处方辅助审核）。
 *
 * <p>消费者购买处方药前需上传处方图片，由执业药师审核通过后方可下单。
 * 图片以 base64 data URL 存于 {@link #imageUrl}，免依赖对象存储，解压即可运行。
 */
public class Prescription {

    private Long id;
    private Long userId;
    private Long drugId;            // 关联的处方药
    private String imageUrl;        // 处方图片 base64 data URL
    private String aiResult;        // AI 预审结果 JSON（预留）
    private Integer status;         // 0待审 1通过 2驳回
    private String reviewComment;   // 药师审核备注
    private Long reviewerId;        // 审核药师
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

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

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAiResult() {
        return aiResult;
    }

    public void setAiResult(String aiResult) {
        this.aiResult = aiResult;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
