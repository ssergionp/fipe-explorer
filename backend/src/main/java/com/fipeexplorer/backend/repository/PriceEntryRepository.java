package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.PriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceEntryRepository extends JpaRepository<PriceEntry, Long> {

    List<PriceEntry> findByVehicleModel_IdOrderByYearCodeAsc(Long vehicleModelId);
}
