package com.fipeexplorer.backend.external;

import com.fipeexplorer.backend.web.VehicleType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FipeExternalApiClient {

    private final RestClient restClient;

    public FipeExternalApiClient(RestClient fipeExternalApiRestClient) {
        this.restClient = fipeExternalApiRestClient;
    }

    public List<FipeHistoryApiResponse.Entry> fetchPriceHistory(VehicleType vehicleType, String fipeCode,
                                                                  String yearCode) {
        try {
            FipeHistoryApiResponse response = restClient.get()
                    .uri("/{vehicleType}/{fipeCode}/years/{yearCode}/history",
                            vehicleType.externalApiSegment(), fipeCode, yearCode)
                    .retrieve()
                    .body(FipeHistoryApiResponse.class);
            return response == null || response.priceHistory() == null ? List.of() : response.priceHistory();
        } catch (HttpClientErrorException.NotFound e) {
            throw new FipeNotFoundException(e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new FipeRateLimitException(e);
        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            throw new FipeUnavailableException(e);
        }
    }
}
