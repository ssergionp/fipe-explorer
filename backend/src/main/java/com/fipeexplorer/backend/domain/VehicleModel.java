package com.fipeexplorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_model")
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "fipe_model_code", nullable = false)
    private String fipeModelCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(name = "fipe_price_code", nullable = false)
    private String fipePriceCode;

    protected VehicleModel() {
    }

    public VehicleModel(Brand brand, String fipeModelCode, String name, String vehicleType, String fipePriceCode) {
        this.brand = brand;
        this.fipeModelCode = fipeModelCode;
        this.name = name;
        this.vehicleType = vehicleType;
        this.fipePriceCode = fipePriceCode;
    }

    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public String getFipeModelCode() {
        return fipeModelCode;
    }

    public String getName() {
        return name;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getFipePriceCode() {
        return fipePriceCode;
    }
}
