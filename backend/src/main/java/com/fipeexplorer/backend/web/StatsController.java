package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.repository.BrandAveragePriceProjection;
import com.fipeexplorer.backend.repository.FuelCountProjection;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleTypeSummaryProjection;
import com.fipeexplorer.backend.web.dto.FuelDistributionDto;
import com.fipeexplorer.backend.web.dto.StatsSummaryDto;
import com.fipeexplorer.backend.web.dto.TopBrandDto;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private static final int DEFAULT_TOP_BRANDS_LIMIT = 10;
    private static final int MAX_TOP_BRANDS_LIMIT = 50;

    private final PriceEntryRepository priceEntryRepository;

    public StatsController(PriceEntryRepository priceEntryRepository) {
        this.priceEntryRepository = priceEntryRepository;
    }

    @GetMapping("/summary")
    public StatsSummaryDto summary(@RequestParam VehicleType type) {
        VehicleTypeSummaryProjection projection = priceEntryRepository.findSummaryByVehicleType(type.name());
        return new StatsSummaryDto(
                projection.getTotalPriceEntries(),
                projection.getDistinctModels(),
                projection.getMinPrice(),
                projection.getAvgPrice(),
                projection.getMaxPrice());
    }

    @GetMapping("/top-brands")
    public List<TopBrandDto> topBrands(
            @RequestParam VehicleType type,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "" + DEFAULT_TOP_BRANDS_LIMIT) int limit) {

        // Sort.Direction.fromOptionalString aceita "asc"/"desc" (e variações de caixa) — o
        // conversor padrão de enum do Spring MVC exigiria "ASC"/"DESC" exatos, o que não bate
        // com o formato de query param mais natural (order=desc|asc).
        Sort.Direction direction = Sort.Direction.fromOptionalString(order)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Valor inválido para 'order': " + order + " (use 'asc' ou 'desc')"));

        int cappedLimit = Math.min(Math.max(limit, 1), MAX_TOP_BRANDS_LIMIT);

        List<BrandAveragePriceProjection> projections = direction.isAscending()
                ? priceEntryRepository.findTopBrandsByAvgPriceAsc(type.name(), cappedLimit)
                : priceEntryRepository.findTopBrandsByAvgPriceDesc(type.name(), cappedLimit);

        return projections.stream()
                .map(p -> new TopBrandDto(p.getBrandId(), p.getBrandName(), p.getAvgPrice(), p.getModelCount()))
                .toList();
    }

    @GetMapping("/fuel-distribution")
    public List<FuelDistributionDto> fuelDistribution(@RequestParam VehicleType type) {
        return priceEntryRepository.findFuelDistributionByVehicleType(type.name()).stream()
                .map(p -> new FuelDistributionDto(p.getFuel(), p.getCount()))
                .toList();
    }
}
