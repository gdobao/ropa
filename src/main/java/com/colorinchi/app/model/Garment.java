package com.colorinchi.app.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "garments")
public class Garment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String category;

    @NotBlank
    @Column(name = "color_name", nullable = false, length = 50)
    private String colorName;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(length = 80)
    private String material;

    @Column(length = 50)
    private String season;

    @NotBlank
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "ai_type", length = 50)
    private String aiType;

    @Column(name = "ai_color_name", length = 50)
    private String aiColorName;

    @Column(name = "ai_color_hex", length = 7)
    private String aiColorHex;

    @Column(name = "ai_confidence", precision = 3, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "ai_model", length = 80)
    private String aiModel;

    @Column(nullable = false)
    private boolean favorite;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "user_confirmed", nullable = false)
    private boolean userConfirmed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
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
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public boolean isUserConfirmed() { return userConfirmed; }
    public void setUserConfirmed(boolean userConfirmed) { this.userConfirmed = userConfirmed; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
