package com.fipeexplorer.backend.domain;

import com.fipeexplorer.backend.web.VehicleCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * "extras" e "components" ficam como JSON serializado em TEXT (ver SavedPriceEstimateService) -
 * mesmo padrão já usado em ExternalPriceHistory.payload. adjustedPrice é sempre recalculado no
 * servidor a partir do PriceEstimateService, nunca aceito do cliente.
 */
@Entity
@Table(name = "saved_price_estimate")
public class SavedPriceEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "price_entry_id", nullable = false)
    private PriceEntry priceEntry;

    @Column(name = "km", nullable = false)
    private long km;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false)
    private VehicleCondition condition;

    @Column(name = "extras", nullable = false)
    private String extrasJson;

    @Column(name = "adjusted_price", nullable = false)
    private BigDecimal adjustedPrice;

    @Column(name = "components", nullable = false)
    private String componentsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedPriceEstimate() {
    }

    public SavedPriceEstimate(User user, PriceEntry priceEntry, long km, VehicleCondition condition,
                               String extrasJson, BigDecimal adjustedPrice, String componentsJson) {
        this.user = user;
        this.priceEntry = priceEntry;
        this.km = km;
        this.condition = condition;
        this.extrasJson = extrasJson;
        this.adjustedPrice = adjustedPrice;
        this.componentsJson = componentsJson;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public PriceEntry getPriceEntry() {
        return priceEntry;
    }

    public long getKm() {
        return km;
    }

    public VehicleCondition getCondition() {
        return condition;
    }

    public String getExtrasJson() {
        return extrasJson;
    }

    public BigDecimal getAdjustedPrice() {
        return adjustedPrice;
    }

    public String getComponentsJson() {
        return componentsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
