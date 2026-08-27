package com.fipeexplorer.backend.repository;

import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.domain.WatchedVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WatchedVehicleRepository extends JpaRepository<WatchedVehicle, Long> {

    Optional<WatchedVehicle> findByUserAndFipeCode(User user, String fipeCode);

    List<WatchedVehicle> findByUserOrderByCreatedAtDesc(User user);

    @Transactional
    void deleteByUserAndFipeCode(User user, String fipeCode);
}
