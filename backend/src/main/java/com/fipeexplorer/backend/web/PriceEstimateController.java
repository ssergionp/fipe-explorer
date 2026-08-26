package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.estimate.PriceEstimateService;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.web.dto.PriceEstimateRequest;
import com.fipeexplorer.backend.web.dto.PriceEstimateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/vehicles")
public class PriceEstimateController {

    private final PriceEntryRepository priceEntryRepository;
    private final PriceEstimateService priceEstimateService;

    public PriceEstimateController(PriceEntryRepository priceEntryRepository,
                                    PriceEstimateService priceEstimateService) {
        this.priceEntryRepository = priceEntryRepository;
        this.priceEstimateService = priceEstimateService;
    }

    @PostMapping("/{priceEntryId}/price-estimate")
    public PriceEstimateResponse estimate(@PathVariable Long priceEntryId,
                                           @Valid @RequestBody PriceEstimateRequest request) {
        PriceEntry priceEntry = priceEntryRepository.findById(priceEntryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registro de preço não encontrado: " + priceEntryId));

        return priceEstimateService.estimate(priceEntry, request);
    }
}
