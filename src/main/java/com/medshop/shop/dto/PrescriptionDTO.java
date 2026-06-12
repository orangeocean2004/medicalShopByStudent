package com.medshop.shop.dto;

/**
 * 处方展示 DTO（我的处方列表 / 药师待审列表）。
 * 联表带出药品名与申请人手机号，避免前端二次查询。
 * 列表场景默认不返回 imageUrl（base64 体积大），详情/审核场景才带图。
 */
public class PrescriptionDTO {

    private Long id;
    private Long userId;
    private String userPhone;
    private Long drugId;
    private String drugName;
    private String imageUrl;       // 仅审核/详情场景返回
    private Integer status;        // 0待审 1通过 2驳回
    private String reviewComment;
    private String createdAt;
    private String reviewedAt;

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

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
