package com.delivery.SuAl.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Configuration
public class MagnetConfig {

    @Bean
    public RestTemplate magnetRestTemplate(
            @Value("${magnet.merchant}") String merchant,
            @Value("${magnet.api-key}") String apiKey
    ) {
        log.info("=== MAGNET Configuration ===");
        log.info("Merchant: [{}]", merchant);
        log.info("API Key length: {}", apiKey.length());

        log.info("API Key character analysis:");
        for (int i = 0; i < Math.min(apiKey.length(), 20); i++) {
            char c = apiKey.charAt(i);
            log.info("  [{}] = '{}' (byte: {})", i, c, (int) c);
        }
        log.info("  ... (showing first 20 chars)");

        byte[] apiKeyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
        log.info("API Key byte length: {}", apiKeyBytes.length);
        log.info("API Key string length: {}", apiKey.length());

        if (apiKeyBytes.length != apiKey.length()) {
            log.warn("WARNING: Byte length differs from string length - multi-byte characters present!");
        }

        log.info("============================");

        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(List.of(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(
                    HttpRequest request,
                    byte[] body,
                    ClientHttpRequestExecution execution) throws IOException {

                request.getHeaders().set("X-Merchant", merchant);
                request.getHeaders().set("X-API-Key", apiKey);
                request.getHeaders().set("X-Type", "JSON");
                request.getHeaders().set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

                log.info("========== MAGNET REQUEST ==========");
                log.info("URL: {}", request.getURI());
                log.info("Method: {}", request.getMethod());

                String sentApiKey = request.getHeaders().getFirst("X-API-Key");
                log.info("X-Merchant: [{}]", request.getHeaders().getFirst("X-Merchant"));
                log.info("X-API-Key length: {}", sentApiKey != null ? sentApiKey.length() : 0);
                log.info("X-Type: [{}]", request.getHeaders().getFirst("X-Type"));

                if (sentApiKey != null && !sentApiKey.equals(apiKey)) {
                    log.error("API KEY MISMATCH DETECTED!");
                    log.error("Original length: {}", apiKey.length());
                    log.error("Header length: {}", sentApiKey.length());
                    log.error("Original first 30 chars: {}", apiKey.substring(0, Math.min(30, apiKey.length())));
                    log.error("Header first 30 chars: {}", sentApiKey.substring(0, Math.min(30, sentApiKey.length())));
                }

                if (sentApiKey != null) {
                    byte[] headerBytes = sentApiKey.getBytes(StandardCharsets.UTF_8);
                    StringBuilder hexDump = new StringBuilder();
                    for (int i = 0; i < Math.min(50, headerBytes.length); i++) {
                        hexDump.append(String.format("%02X ", headerBytes[i]));
                    }
                    log.info("X-API-Key first 50 bytes (hex): {}", hexDump);
                }

                log.info("====================================");

                ClientHttpResponse response = execution.execute(request, body);

                log.info("Response status: {}", response.getStatusCode());

                return response;
            }
        }));

        return restTemplate;
    }
}