package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.exception.GatewayException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.PaymentCreationException;
import com.delivery.SuAl.mapper.MagnetGatewayMapper;
import com.delivery.SuAl.mapper.PaymentMapper;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.response.payment.CreatePaymentResponse;
import com.delivery.SuAl.model.response.payment.PaymentStatusResponse;
import com.delivery.SuAl.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagnetPaymentService implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MagnetGatewayMapper gatewayMapper;
    private final RestTemplate magnetRestTemplate;

    @Value("${magnet.api.base-url}")
    private String baseUrl;

    @Value("${magnet.biller}")
    private String biller;

    @Value("${magnet.template}")
    private String template;

    @Value("${magnet.callback-url}")
    private String callbackUrl;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Override
    @Transactional
    public PaymentDTO initialize(CreatePaymentDTO dto, Order order) {

        Payment payment = paymentMapper.toEntity(dto);
        payment.setOrder(order);
        payment.setReferenceId(generateReference(order));
        payment.setPaymentStatus(PaymentStatus.CREATED);

        payment = paymentRepository.save(payment);

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/payment/create")
                .queryParam("reference", payment.getReferenceId())
                .queryParam("amount", payment.getAmountInCoins())
                .queryParam("currency", payment.getCurrencyCode())
                .queryParam("biller", biller)
                .queryParam("template", template)
                .queryParam("language", dto.getLanguage() != null ? dto.getLanguage() : "az")
                .queryParam("type", payment.getTransactionType().name())
                .queryParam("callback", callbackUrl)
                .queryParam("description", dto.getDescription() != null ? dto.getDescription() : "Payment")
                .toUriString();

        try {
            log.info("MAGNET create payment URL: {}", url);

            ResponseEntity<String> response = magnetRestTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.info("MAGNET raw response: {}", body);
            log.info("MAGNET response status: {}", response.getStatusCode());

            if (body == null || body.isBlank()) {
                failPayment(payment, null, "Empty response from MAGNET");
                throw new GatewayException("Empty response from MAGNET");
            }

            if (body.contains("<html") || body.contains("<!DOCTYPE")) {
                log.error("HTML response received: {}", body);
                failPayment(payment, null, "HTML response from MAGNET - possible authentication error");
                throw new GatewayException("Invalid response format - check API credentials");
            }

            CreatePaymentResponse gatewayResponse;
            try {
                gatewayResponse = objectMapper.readValue(body, CreatePaymentResponse.class);
            } catch (JsonProcessingException e) {
                log.error("JSON parsing failed for body: {}", body, e);
                failPayment(payment, null, "Invalid JSON response");
                throw new GatewayException("Failed to parse MAGNET response");
            }

            log.info("Parsed response - code: {}, message: {}, url: {}",
                    gatewayResponse.getCode(),
                    gatewayResponse.getMessage(),
                    gatewayResponse.getUrl());

            if (gatewayResponse.getCode() == null || gatewayResponse.getCode() != 0) {
                String errorMsg = gatewayResponse.getMessage() != null
                        ? gatewayResponse.getMessage()
                        : "Unknown error";
                failPayment(payment, gatewayResponse, "MAGNET error: " + errorMsg);
                throw new GatewayException("MAGNET create payment failed: " + errorMsg);
            }

            if (gatewayResponse.getUrl() == null || gatewayResponse.getUrl().isBlank()) {
                failPayment(payment, gatewayResponse, "No payment URL received");
                throw new GatewayException("No payment URL in MAGNET response");
            }

            payment.setGatewayPaymentUrl(gatewayResponse.getUrl());
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment.setRawCreateResponse(body);
            paymentRepository.save(payment);

            log.info("Payment initialized successfully: {}", payment.getReferenceId());

            return paymentMapper.toDto(payment);

        } catch (GatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("MAGNET create payment failed unexpectedly", ex);
            failPayment(payment, null, "System error: " + ex.getMessage());
            throw new PaymentCreationException("Payment initialization failed", ex);
        }
    }


    @Override
    @Transactional
    public void handleCallback(String reference) {

        Payment payment = paymentRepository.findByReferenceId(reference)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + reference));

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/payment/status")
                .queryParam("reference", reference)
                .toUriString();

        try {
            log.info("MAGNET status check URL: {}", url);

            ResponseEntity<String> response = magnetRestTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.info("MAGNET status response: {}", body);

            if (body == null || body.isBlank()) {
                log.error("Empty status response for reference: {}", reference);
                return;
            }

            PaymentStatusResponse status;
            try {
                status = objectMapper.readValue(body, PaymentStatusResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse status response: {}", body, e);
                return;
            }

            log.info("Parsed status - code: {}, status: {}, message: {}",
                    status.getCode(),
                    status.getStatus(),
                    status.getMessage());

            gatewayMapper.updatePaymentFromStatusResponse(status, payment);
            payment.setRawStatusResponse(body);
            paymentRepository.save(payment);

            log.info("Payment status updated: {} -> {}", reference, payment.getPaymentStatus());

        } catch (Exception ex) {
            log.error("MAGNET status check failed for {}", reference, ex);
        }
    }


    private void failPayment(Payment payment, CreatePaymentResponse response, String fallbackReason) {
        payment.setPaymentStatus(PaymentStatus.FAILED);

        if (response != null) {
            payment.setGatewayResponseCode(
                    response.getCode() != null ? response.getCode().toString() : null
            );
            payment.setFailureReason(response.getMessage());
        } else {
            payment.setFailureReason(fallbackReason != null ? fallbackReason : "MAGNET response is null");
        }

        paymentRepository.save(payment);
        log.warn("Payment failed: {} - {}", payment.getReferenceId(), payment.getFailureReason());
    }

    private String generateReference(Order order) {
        return "PAY-%s-%s-%s".formatted(
                order.getOrderNumber(),
                LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}