package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByFipeCode(String fipeCode);

    Optional<Brand> findByName(String name);

    @Query("select distinct vm.brand from VehicleModel vm where vm.vehicleType = :type order by vm.brand.name")
    List<Brand> findDistinctByVehicleModelsVehicleType(@Param("type") String type);
}
