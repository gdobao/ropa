package com.colorinchi.app.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public class GarmentReviewForm {

    private Long id;
    @NotBlank
    private String imageUrl;
    @NotBlank
    private String name;
    @NotBlank
    private String category;
    @NotBlank
    private String colorName;
    private String colorHex;
    private String material;
    private String season;
    private String aiType;
    private String aiColorName;
    private String aiColorHex;
    private BigDecimal aiConfidence;
    private String aiModel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getAiType() { return aiType; }
    public void setAiType(String aiType) { this.aiType = aiType; }
    public String getAiColorName() { return aiColorName; }
    public void setAiColorName(String aiColorName) { this.aiColorName = aiColorName; }
    public String getAiColorHex() { return aiColorHex; }
    public void setAiColorHex(String aiColorHex) { this.aiColorHex = aiColorHex; }
    public BigDecimal getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(BigDecimal aiConfidence) { this.aiConfidence = aiConfidence; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
}
