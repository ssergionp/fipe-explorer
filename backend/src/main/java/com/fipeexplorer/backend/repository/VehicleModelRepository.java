package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    Optional<VehicleModel> findByBrandIdAndFipeModelCode(Long brandId, String fipeModelCode);

    Optional<VehicleModel> findByFipePriceCode(String fipePriceCode);

    @Query("select distinct vm.vehicleType from VehicleModel vm order by vm.vehicleType")
    List<String> findDistinctVehicleTypes();

    List<VehicleModel> findByBrand_IdAndVehicleTypeOrderByNameAsc(Long brandId, String vehicleType);
}
