package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuelTypeRepository extends JpaRepository<FuelType, Long> {

    Optional<FuelType> findByName(String name);
}
