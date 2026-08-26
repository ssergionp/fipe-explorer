package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.web.dto.PageResponseDto;
import com.fipeexplorer.backend.web.dto.VehicleSearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                .map(VehicleSearchResultDto::from);

        return PageResponseDto.from(results);
    }

    /**
     * ids fora do banco são ignorados silenciosamente (nunca derrubam a resposta inteira) — só
     * a contagem de ids pedidos (2 a 4) é validada. A ordem de retorno segue a ordem pedida em
     * {@code ids}, não a ordem de leitura do banco.
     */
    @GetMapping("/compare")
    public List<VehicleSearchResultDto> compare(@RequestParam List<Long> ids) {
        if (ids.size() < 2 || ids.size() > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe entre 2 e 4 ids para comparar (recebido: " + ids.size() + ")");
        }

        Map<Long, PriceEntry> entriesById = priceEntryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PriceEntry::getId, Function.identity()));

        return ids.stream()
                .map(entriesById::get)
                .filter(Objects::nonNull)
                .map(VehicleSearchResultDto::from)
                .toList();
    }

    private static String sortProperty(VehicleSearchSortBy sortBy) {
        return switch (sortBy) {
            case PRICE -> "price";
            case MODEL_NAME -> "vehicleModel.name";
        };
    }
}
