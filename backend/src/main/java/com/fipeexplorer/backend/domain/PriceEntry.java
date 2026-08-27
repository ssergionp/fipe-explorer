package com.fipeexplorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_entry")
public class PriceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    private VehicleModel vehicleModel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fuel_type_id", nullable = false)
    private FuelType fuelType;

    @Column(name = "year_code", nullable = false)
    private String yearCode;

    @Column(name = "year_value", nullable = false)
    private String yearValue;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "reference_month", nullable = false)
    private String referenceMonth;

    @Column(name = "reference_month_key", nullable = false)
    private LocalDate referenceMonthKey;

    protected PriceEntry() {
    }

    public PriceEntry(VehicleModel vehicleModel, FuelType fuelType, String yearCode, String yearValue,
                       BigDecimal price, String referenceMonth, LocalDate referenceMonthKey) {
        this.vehicleModel = vehicleModel;
        this.fuelType = fuelType;
        this.yearCode = yearCode;
        this.yearValue = yearValue;
        this.price = price;
        this.referenceMonth = referenceMonth;
        this.referenceMonthKey = referenceMonthKey;
    }

    public Long getId() {
        return id;
    }

    public VehicleModel getVehicleModel() {
        return vehicleModel;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public String getYearCode() {
        return yearCode;
    }

    public String getYearValue() {
        return yearValue;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public LocalDate getReferenceMonthKey() {
        return referenceMonthKey;
    }
}
