package com.fipeexplorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * fipe_code, não price_entry_id/vehicle_model_id diretamente - observa "o veículo" (marca+modelo,
 * que é o que um fipe_code identifica de forma única desde a V10), não uma linha específica de
 * ano/combustível. threshold_percent é fração (0.05 = 5%), mesma convenção do resto do projeto.
 */
@Entity
@Table(name = "watched_vehicle", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "fipe_code"}))
public class WatchedVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fipe_code", nullable = false)
    private String fipeCode;

    @Column(name = "threshold_percent", nullable = false)
    private BigDecimal thresholdPercent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WatchedVehicle() {
    }

    public WatchedVehicle(User user, String fipeCode, BigDecimal thresholdPercent) {
        this.user = user;
        this.fipeCode = fipeCode;
        this.thresholdPercent = thresholdPercent;
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

    public String getFipeCode() {
        return fipeCode;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
