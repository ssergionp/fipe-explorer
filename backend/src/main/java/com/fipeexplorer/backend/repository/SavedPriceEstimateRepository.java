package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.SavedPriceEstimate;
import com.fipeexplorer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedPriceEstimateRepository extends JpaRepository<SavedPriceEstimate, Long> {

    @Query("select s from SavedPriceEstimate s "
            + "join fetch s.priceEntry pe "
            + "join fetch pe.vehicleModel vm "
            + "join fetch vm.brand "
            + "join fetch pe.fuelType "
            + "where s.user = :user order by s.createdAt desc")
    List<SavedPriceEstimate> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    Optional<SavedPriceEstimate> findByIdAndUser(Long id, User user);
}
