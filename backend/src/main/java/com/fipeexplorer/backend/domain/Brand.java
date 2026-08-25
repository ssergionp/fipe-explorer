package com.fipeexplorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "brand")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fipe_code", nullable = false, unique = true)
    private String fipeCode;

    @Column(name = "name", nullable = false)
    private String name;

    protected Brand() {
    }

    public Brand(String fipeCode, String name) {
        this.fipeCode = fipeCode;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getFipeCode() {
        return fipeCode;
    }

    public String getName() {
        return name;
    }
}
