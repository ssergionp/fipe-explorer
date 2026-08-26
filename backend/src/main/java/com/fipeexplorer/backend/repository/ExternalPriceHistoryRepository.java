package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.ExternalPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalPriceHistoryRepository extends JpaRepository<ExternalPriceHistory, Long> {

    Optional<ExternalPriceHistory> findByVehicleTypeAndFipeCodeAndYearCode(
            String vehicleType, String fipeCode, String yearCode);
}
