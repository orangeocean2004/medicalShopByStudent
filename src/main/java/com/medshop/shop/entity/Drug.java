package com.medshop.shop.entity;

import java.math.BigDecimal;

/**
 * 药品实体，对应 drug 表。
 */
public class Drug {

    private Long id;
    private String name;
    private String category;
    private Integer isRx;          // 0非处方 1处方
    private BigDecimal price;
    private Integer stock;
    private String indication;     // 适应症
    private String dosage;         // 用法用量
    private String contraindication; // 禁忌
    private String imageUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getIsRx() {
        return isRx;
    }

    public void setIsRx(Integer isRx) {
        this.isRx = isRx;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getIndication() {
        return indication;
    }

    public void setIndication(String indication) {
        this.indication = indication;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getContraindication() {
        return contraindication;
    }

    public void setContraindication(String contraindication) {
        this.contraindication = contraindication;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
