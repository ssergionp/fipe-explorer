package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import com.fipeexplorer.backend.web.dto.ModelPriceHistoryResponse;
import com.fipeexplorer.backend.web.dto.PricePointDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class PriceController {

    private final VehicleModelRepository vehicleModelRepository;
    private final PriceEntryRepository priceEntryRepository;

    public PriceController(VehicleModelRepository vehicleModelRepository,
                            PriceEntryRepository priceEntryRepository) {
        this.vehicleModelRepository = vehicleModelRepository;
        this.priceEntryRepository = priceEntryRepository;
    }

    @GetMapping("/{modelId}/prices")
    public ModelPriceHistoryResponse getPricesByModel(@PathVariable Long modelId) {
        VehicleModel vehicleModel = vehicleModelRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Modelo não encontrado: " + modelId));

        List<PricePointDto> prices = priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(modelId)
                .stream()
                .map(PriceController::toDto)
                .toList();

        return new ModelPriceHistoryResponse(
                vehicleModel.getId(),
                vehicleModel.getBrand().getName(),
                vehicleModel.getName(),
                prices);
    }

    private static PricePointDto toDto(PriceEntry entry) {
        return new PricePointDto(
                entry.getYearCode(),
                entry.getYearValue(),
                entry.getFuelType().getName(),
                entry.getPrice());
    }
}
