package com.fipeexplorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "external_price_history")
public class ExternalPriceHistory {

    public enum Status {
        AVAILABLE,
        NOT_FOUND
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(name = "fipe_code", nullable = false)
    private String fipeCode;

    @Column(name = "year_code", nullable = false)
    private String yearCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "payload")
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected ExternalPriceHistory() {
    }

    public ExternalPriceHistory(String vehicleType, String fipeCode, String yearCode, Status status,
                                 String payload, Instant fetchedAt) {
        this.vehicleType = vehicleType;
        this.fipeCode = fipeCode;
        this.yearCode = yearCode;
        this.status = status;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getFipeCode() {
        return fipeCode;
    }

    public String getYearCode() {
        return yearCode;
    }

    public Status getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void update(Status status, String payload, Instant fetchedAt) {
        this.status = status;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }
}
