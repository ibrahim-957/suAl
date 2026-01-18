package com.delivery.SuAl.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class MagnetConfig {

    @Bean
    public RestTemplate magnetRestTemplate(
            @Value("${magnet.merchant}") String merchant,
            @Value("${magnet.api-key}") String apiKey,
            @Value("${magnet.connection-timeout:10000}") int connectionTimeout,
            @Value("${magnet.read-timeout:20000}") int readTimeout
    ) {
        log.info("=== MAGNET Configuration ===");
        log.info("Merchant: [{}]", merchant);
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : 0);
        log.info("API Key starts with: {}", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) : "null");
        log.info("API Key ends with: {}", apiKey != null ? apiKey.substring(Math.max(0, apiKey.length() - 10)) : "null");
        log.info("============================");

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                .setResponseTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate =
                new RestTemplate(new BufferingClientHttpRequestFactory(factory));

        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            String trimmedMerchant = merchant.trim();
            String trimmedApiKey = apiKey.trim();

            request.getHeaders().set("X-Merchant", trimmedMerchant);
            request.getHeaders().set("X-API-Key", trimmedApiKey);
            request.getHeaders().set("X-Type", "JSON");
            request.getHeaders().set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            // DETAILED LOGGING
            log.info("========== MAGNET REQUEST ==========");
            log.info("Method: {} {}", request.getMethod(), request.getURI());
            log.info("X-Merchant header: [{}]", request.getHeaders().getFirst("X-Merchant"));
            log.info("X-API-Key header length: {}", request.getHeaders().getFirst("X-API-Key") != null ? request.getHeaders().getFirst("X-API-Key").length() : 0);
            log.info("X-API-Key header value: [{}]", request.getHeaders().getFirst("X-API-Key"));
            log.info("X-Type header: [{}]", request.getHeaders().getFirst("X-Type"));
            log.info("All headers: {}", request.getHeaders());
            log.info("====================================");

            ClientHttpResponse response = execution.execute(request, body);

            log.info("Response status: {}", response.getStatusCode());

            return response;
        }));

        return restTemplate;
    }
}