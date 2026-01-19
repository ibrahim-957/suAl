package com.delivery.SuAl.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Configuration
public class MagnetConfig {

    @Bean
    public RestTemplate magnetRestTemplate(
            @Value("${magnet.merchant}") String merchant,
            @Value("${magnet.api-key}") String apiKey) {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(List.of((request, body, execution) -> {

            request.getHeaders().set("X-Merchant", merchant);
            request.getHeaders().set("X-API-Key", apiKey);
            request.getHeaders().set("X-Type", "JSON");
            request.getHeaders().set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            ClientHttpResponse response = execution.execute(request, body);

            log.info("Response status: {}", response.getStatusCode());

            return response;
        }));

        return restTemplate;
    }
}