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

import java.time.Instant;

@Entity
@Table(name = "favorite_vehicle", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "price_entry_id"}))
public class FavoriteVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "price_entry_id", nullable = false)
    private PriceEntry priceEntry;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FavoriteVehicle() {
    }

    public FavoriteVehicle(User user, PriceEntry priceEntry) {
        this.user = user;
        this.priceEntry = priceEntry;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
