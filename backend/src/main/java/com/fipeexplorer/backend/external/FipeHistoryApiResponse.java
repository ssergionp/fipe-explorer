package com.fipeexplorer.backend.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Espelha a resposta de GET /{vehicleType}/{fipeCode}/years/{yearId}/history da API v2
 * (fipe.parallelum.com.br) — verificado com uma chamada real (Fiat Elba, fipeCode 001025-1,
 * yearId 1996-1/1996-2) batendo com os preços já importados no nosso CSV.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FipeHistoryApiResponse(
        @JsonProperty("codeFipe") String fipeCode,
        String fuel,
        List<Entry> priceHistory
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String month, String price, String reference) {
    }
}
