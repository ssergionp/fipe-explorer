package com.fipeexplorer.backend.estimate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.SavedPriceEstimate;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.SavedPriceEstimateRepository;
import com.fipeexplorer.backend.web.VehicleExtra;
import com.fipeexplorer.backend.web.dto.PriceAdjustmentComponentDto;
import com.fipeexplorer.backend.web.dto.PriceEstimateRequest;
import com.fipeexplorer.backend.web.dto.PriceEstimateResponse;
import com.fipeexplorer.backend.web.dto.SavePriceEstimateRequest;
import com.fipeexplorer.backend.web.dto.SavedPriceEstimateDto;
import com.fipeexplorer.backend.web.dto.VehicleSearchResultDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** "Salvar uma estimativa calculada" - distinto do endpoint público de calculadora rápida. */
@Service
public class SavedPriceEstimateService {

    private static final TypeReference<List<String>> EXTRAS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<PriceAdjustmentComponentDto>> COMPONENTS_TYPE = new TypeReference<>() {
    };

    private final SavedPriceEstimateRepository savedPriceEstimateRepository;
    private final PriceEntryRepository priceEntryRepository;
    private final PriceEstimateService priceEstimateService;
    private final ObjectMapper objectMapper;

    public SavedPriceEstimateService(SavedPriceEstimateRepository savedPriceEstimateRepository,
                                      PriceEntryRepository priceEntryRepository,
                                      PriceEstimateService priceEstimateService,
                                      ObjectMapper objectMapper) {
        this.savedPriceEstimateRepository = savedPriceEstimateRepository;
        this.priceEntryRepository = priceEntryRepository;
        this.priceEstimateService = priceEstimateService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SavedPriceEstimateDto save(User user, SavePriceEstimateRequest request) {
        PriceEntry priceEntry = priceEntryRepository.findById(request.priceEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registro de preço não encontrado: " + request.priceEntryId()));

        List<VehicleExtra> extras = request.extras() == null ? List.of() : request.extras();
        PriceEstimateRequest estimateRequest = new PriceEstimateRequest(request.km(), request.condition(), extras);

        // Nunca confia em preço calculado pelo cliente - recalcula do zero com o mesmo serviço
        // usado pela calculadora pública (PriceEstimateService).
        PriceEstimateResponse estimate = priceEstimateService.estimate(priceEntry, estimateRequest);

        List<String> extraKeys = extras.stream().map(Enum::name).toList();
        SavedPriceEstimate saved = new SavedPriceEstimate(
                user, priceEntry, request.km(), request.condition(),
                writeJson(extraKeys), estimate.adjustedPrice(), writeJson(estimate.components()));
        saved = savedPriceEstimateRepository.save(saved);

        return toDto(saved, estimate.components());
    }

    public List<SavedPriceEstimateDto> list(User user) {
        return savedPriceEstimateRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toDto)
                .toList();
    }

    /** @return true se algo foi removido; false se o id não existe ou não pertence ao usuário. */
    @Transactional
    public boolean delete(User user, Long id) {
        return savedPriceEstimateRepository.findByIdAndUser(id, user)
                .map(saved -> {
                    savedPriceEstimateRepository.delete(saved);
                    return true;
                })
                .orElse(false);
    }

    private SavedPriceEstimateDto toDto(SavedPriceEstimate saved) {
        return toDto(saved, readJson(saved.getComponentsJson(), COMPONENTS_TYPE));
    }

    private SavedPriceEstimateDto toDto(SavedPriceEstimate saved, List<PriceAdjustmentComponentDto> components) {
        return new SavedPriceEstimateDto(
                saved.getId(),
                VehicleSearchResultDto.from(saved.getPriceEntry()),
                saved.getKm(),
                saved.getCondition(),
                readJson(saved.getExtrasJson(), EXTRAS_TYPE),
                saved.getPriceEntry().getPrice(),
                saved.getAdjustedPrice(),
                components,
                saved.getCreatedAt());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new UncheckedIOException("Falha ao serializar estimativa salva", new IOException(e));
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new UncheckedIOException("Falha ao ler estimativa salva", new IOException(e));
        }
    }
}
