package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.FuelType;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.FuelTypeRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import com.fipeexplorer.backend.web.dto.BrandDto;
import com.fipeexplorer.backend.web.dto.FuelTypeDto;
import com.fipeexplorer.backend.web.dto.VehicleModelSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final BrandRepository brandRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final FuelTypeRepository fuelTypeRepository;
    private final PriceEntryRepository priceEntryRepository;

    public CatalogController(BrandRepository brandRepository,
                              VehicleModelRepository vehicleModelRepository,
                              FuelTypeRepository fuelTypeRepository,
                              PriceEntryRepository priceEntryRepository) {
        this.brandRepository = brandRepository;
        this.vehicleModelRepository = vehicleModelRepository;
        this.fuelTypeRepository = fuelTypeRepository;
        this.priceEntryRepository = priceEntryRepository;
    }

    @GetMapping("/vehicle-types")
    public List<String> getVehicleTypes() {
        return vehicleModelRepository.findDistinctVehicleTypes();
    }

    @GetMapping("/vehicle-types/{type}/years")
    public List<Integer> getYearsByVehicleType(@PathVariable VehicleType type) {
        return priceEntryRepository.findDistinctYearsByVehicleType(type.name());
    }

    @GetMapping("/fuel-types")
    public List<FuelTypeDto> getFuelTypes() {
        return fuelTypeRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CatalogController::toDto)
                .toList();
    }

    @GetMapping("/brands")
    public List<BrandDto> getBrands(@RequestParam VehicleType type) {
        return brandRepository.findDistinctByVehicleModelsVehicleType(type.name())
                .stream()
                .map(CatalogController::toDto)
                .toList();
    }

    @GetMapping("/brands/{brandId}/models")
    public List<VehicleModelSummaryDto> getModelsByBrand(@PathVariable Long brandId,
                                                           @RequestParam VehicleType type) {
        if (!brandRepository.existsById(brandId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca não encontrada: " + brandId);
        }

        return vehicleModelRepository.findByBrand_IdAndVehicleTypeOrderByNameAsc(brandId, type.name())
                .stream()
                .map(CatalogController::toDto)
                .toList();
    }

    private static BrandDto toDto(Brand brand) {
        return new BrandDto(brand.getId(), brand.getName());
    }

    private static VehicleModelSummaryDto toDto(VehicleModel model) {
        return new VehicleModelSummaryDto(model.getId(), model.getName(), model.getVehicleType());
    }

    private static FuelTypeDto toDto(FuelType fuelType) {
        return new FuelTypeDto(fuelType.getId(), fuelType.getCode(), fuelType.getName());
    }
}
