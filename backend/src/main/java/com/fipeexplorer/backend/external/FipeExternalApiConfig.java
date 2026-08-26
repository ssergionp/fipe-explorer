package com.fipeexplorer.backend.external;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FipeExternalApiConfig {

    @Bean
    RestClient fipeExternalApiRestClient(FipeExternalApiProperties properties) {
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            builder.defaultHeader("X-Subscription-Token", properties.getToken());
        }
        return builder.build();
    }
}
