package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.web.dto.PageResponseDto;
import com.fipeexplorer.backend.web.dto.VehicleSearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleSearchController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PriceEntryRepository priceEntryRepository;

    public VehicleSearchController(PriceEntryRepository priceEntryRepository) {
        this.priceEntryRepository = priceEntryRepository;
    }

    @GetMapping("/search")
    public PageResponseDto<VehicleSearchResultDto> search(
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "MODEL_NAME") VehicleSearchSortBy sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {

        Specification<PriceEntry> spec = Specification.where(null);
        if (type != null) {
            spec = spec.and(PriceEntrySpecifications.hasVehicleType(type.name()));
        }
        if (brandId != null) {
            spec = spec.and(PriceEntrySpecifications.hasBrandId(brandId));
        }
        if (modelId != null) {
            spec = spec.and(PriceEntrySpecifications.hasModelId(modelId));
        }
        if (year != null) {
            spec = spec.and(PriceEntrySpecifications.hasYear(year));
        }
        if (fuel != null && !fuel.isBlank()) {
            spec = spec.and(PriceEntrySpecifications.hasFuel(fuel));
        }
        if (minPrice != null) {
            spec = spec.and(PriceEntrySpecifications.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(PriceEntrySpecifications.priceLessThanOrEqual(maxPrice));
        }

        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageNumber = Math.max(page, 0);
        Sort sort = Sort.by(sortDir, sortProperty(sortBy));
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sort);

        Page<VehicleSearchResultDto> results = priceEntryRepository.findAll(spec, pageRequest)
                .map(VehicleSearchController::toDto);

        return PageResponseDto.from(results);
    }

    private static String sortProperty(VehicleSearchSortBy sortBy) {
        return switch (sortBy) {
            case PRICE -> "price";
            case MODEL_NAME -> "vehicleModel.name";
        };
    }

    private static VehicleSearchResultDto toDto(PriceEntry entry) {
        return new VehicleSearchResultDto(
                entry.getVehicleModel().getId(),
                entry.getVehicleModel().getBrand().getName(),
                entry.getVehicleModel().getName(),
                yearFromYearCode(entry.getYearCode()),
                entry.getFuelType().getName(),
                entry.getPrice(),
                entry.getVehicleModel().getFipePriceCode());
    }

    private static String yearFromYearCode(String yearCode) {
        int dashIndex = yearCode.indexOf('-');
        return dashIndex >= 0 ? yearCode.substring(0, dashIndex) : yearCode;
    }
}
