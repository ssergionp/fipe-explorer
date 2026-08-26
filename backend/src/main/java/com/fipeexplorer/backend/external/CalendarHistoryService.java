package com.fipeexplorer.backend.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.ExternalPriceHistory;
import com.fipeexplorer.backend.importer.FipePriceParser;
import com.fipeexplorer.backend.repository.ExternalPriceHistoryRepository;
import com.fipeexplorer.backend.web.VehicleType;
import com.fipeexplorer.backend.web.dto.CalendarHistoryPointDto;
import com.fipeexplorer.backend.web.dto.CalendarHistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarHistoryService {

    private static final Logger log = LoggerFactory.getLogger(CalendarHistoryService.class);
    private static final TypeReference<List<CalendarHistoryPointDto>> MONTHS_TYPE = new TypeReference<>() {
    };

    private final ExternalPriceHistoryRepository repository;
    private final FipeExternalApiClient client;
    private final FipeExternalApiProperties properties;
    private final ObjectMapper objectMapper;

    public CalendarHistoryService(ExternalPriceHistoryRepository repository, FipeExternalApiClient client,
                                   FipeExternalApiProperties properties, ObjectMapper objectMapper) {
        this.repository = repository;
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CalendarHistoryResponse getHistory(VehicleType vehicleType, String fipeCode, String yearCode) {
        Optional<ExternalPriceHistory> cached = repository.findByVehicleTypeAndFipeCodeAndYearCode(
                vehicleType.name(), fipeCode, yearCode);

        if (cached.isPresent() && !isExpired(cached.get())) {
            return fromCache(cached.get());
        }

        try {
            List<CalendarHistoryPointDto> months = client.fetchPriceHistory(vehicleType, fipeCode, yearCode)
                    .stream()
                    .map(CalendarHistoryService::toPoint)
                    .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.referenceCode())))
                    .toList();
            saveCache(cached, vehicleType, fipeCode, yearCode, ExternalPriceHistory.Status.AVAILABLE, writePayload(months));
            return new CalendarHistoryResponse(CalendarHistoryResponse.Status.AVAILABLE, null, false, months);
        } catch (FipeNotFoundException e) {
            saveCache(cached, vehicleType, fipeCode, yearCode, ExternalPriceHistory.Status.NOT_FOUND, null);
            return new CalendarHistoryResponse(CalendarHistoryResponse.Status.NOT_FOUND,
                    "Este veículo não foi encontrado na base da FIPE (pode haver divergência entre a nossa base e a deles).",
                    false, List.of());
        } catch (FipeRateLimitException e) {
            log.warn("Cota da API externa da FIPE excedida ao buscar {}/{}/{}", vehicleType, fipeCode, yearCode);
            return new CalendarHistoryResponse(CalendarHistoryResponse.Status.RATE_LIMITED,
                    "Limite diário de consultas à FIPE foi atingido. Tente novamente mais tarde.", false, List.of());
        } catch (FipeUnavailableException e) {
            log.warn("Falha ao consultar a API externa da FIPE para {}/{}/{}: {}",
                    vehicleType, fipeCode, yearCode, e.getMessage());
            return new CalendarHistoryResponse(CalendarHistoryResponse.Status.UNAVAILABLE,
                    "Não foi possível conectar à API da FIPE agora. Tente novamente em instantes.", false, List.of());
        }
    }

    private boolean isExpired(ExternalPriceHistory row) {
        return row.getFetchedAt().isBefore(Instant.now().minus(properties.getCacheTtlHours(), ChronoUnit.HOURS));
    }

    private CalendarHistoryResponse fromCache(ExternalPriceHistory row) {
        if (row.getStatus() == ExternalPriceHistory.Status.NOT_FOUND) {
            return new CalendarHistoryResponse(CalendarHistoryResponse.Status.NOT_FOUND,
                    "Este veículo não foi encontrado na base da FIPE (pode haver divergência entre a nossa base e a deles).",
                    true, List.of());
        }
        return new CalendarHistoryResponse(CalendarHistoryResponse.Status.AVAILABLE, null, true, readPayload(row.getPayload()));
    }

    private void saveCache(Optional<ExternalPriceHistory> existing, VehicleType vehicleType, String fipeCode,
                            String yearCode, ExternalPriceHistory.Status status, String payload) {
        Instant now = Instant.now();
        if (existing.isPresent()) {
            existing.get().update(status, payload, now);
        } else {
            repository.save(new ExternalPriceHistory(vehicleType.name(), fipeCode, yearCode, status, payload, now));
        }
    }

    private static CalendarHistoryPointDto toPoint(FipeHistoryApiResponse.Entry entry) {
        return new CalendarHistoryPointDto(entry.month(), entry.reference(), FipePriceParser.parse(entry.price()));
    }

    private String writePayload(List<CalendarHistoryPointDto> months) {
        try {
            return objectMapper.writeValueAsString(months);
        } catch (Exception e) {
            throw new UncheckedIOException("Falha ao serializar cache de histórico de preço", new java.io.IOException(e));
        }
    }

    private List<CalendarHistoryPointDto> readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(payload, MONTHS_TYPE);
        } catch (Exception e) {
            log.warn("Falha ao ler cache de histórico de preço, ignorando linha cacheada: {}", e.getMessage());
            return List.of();
        }
    }
}
