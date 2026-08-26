package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.FavoriteVehicle;
import com.fipeexplorer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FavoriteVehicleRepository extends JpaRepository<FavoriteVehicle, Long> {

    Optional<FavoriteVehicle> findByUserAndPriceEntry_Id(User user, Long priceEntryId);

    @Query("select f from FavoriteVehicle f "
            + "join fetch f.priceEntry pe "
            + "join fetch pe.vehicleModel vm "
            + "join fetch vm.brand "
            + "join fetch pe.fuelType "
            + "where f.user = :user order by f.createdAt desc")
    List<FavoriteVehicle> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Transactional
    void deleteByUserAndPriceEntry_Id(User user, Long priceEntryId);
}
