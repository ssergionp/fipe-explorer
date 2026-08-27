package com.fipeexplorer.backend.alerts;

import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.domain.WatchedVehicle;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import com.fipeexplorer.backend.repository.WatchedVehicleRepository;
import com.fipeexplorer.backend.web.dto.WatchedVehicleDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Observar é upsert, não idempotente-ignora como favoritar: threshold é um dado com valor
 * (diferente de um favorito, que só existe ou não existe), então observar de novo com um
 * threshold diferente atualiza o threshold em vez de ignorar - não existe um endpoint de edição
 * separado.
 */
@Service
public class WatchedVehicleService {

    private static final BigDecimal DEFAULT_THRESHOLD_PERCENT = new BigDecimal("0.05");

    private final WatchedVehicleRepository watchedVehicleRepository;
    private final VehicleModelRepository vehicleModelRepository;

    public WatchedVehicleService(WatchedVehicleRepository watchedVehicleRepository,
                                  VehicleModelRepository vehicleModelRepository) {
        this.watchedVehicleRepository = watchedVehicleRepository;
        this.vehicleModelRepository = vehicleModelRepository;
    }

    @Transactional
    public WatchedVehicleDto watch(User user, String fipeCode, BigDecimal thresholdPercent) {
        VehicleModel vehicleModel = vehicleModelRepository.findByFipePriceCode(fipeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Veículo não encontrado pro código FIPE: " + fipeCode));

        BigDecimal effectiveThreshold = thresholdPercent != null ? thresholdPercent : DEFAULT_THRESHOLD_PERCENT;

        WatchedVehicle watch = watchedVehicleRepository.findByUserAndFipeCode(user, fipeCode)
                .map(existing -> {
                    existing.setThresholdPercent(effectiveThreshold);
                    return existing;
                })
                .orElseGet(() -> new WatchedVehicle(user, fipeCode, effectiveThreshold));

        watch = watchedVehicleRepository.save(watch);
        return toDto(watch, vehicleModel);
    }

    public List<WatchedVehicleDto> listWatched(User user) {
        return watchedVehicleRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(watch -> toDto(watch, vehicleModelRepository.findByFipePriceCode(watch.getFipeCode()).orElse(null)))
                .toList();
    }

    @Transactional
    public void unwatch(User user, String fipeCode) {
        watchedVehicleRepository.deleteByUserAndFipeCode(user, fipeCode);
    }

    private WatchedVehicleDto toDto(WatchedVehicle watch, VehicleModel model) {
        return new WatchedVehicleDto(
                watch.getId(),
                watch.getFipeCode(),
                model != null ? model.getBrand().getName() : null,
                model != null ? model.getName() : null,
                watch.getThresholdPercent(),
                watch.getCreatedAt());
    }
}
