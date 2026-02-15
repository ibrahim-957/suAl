package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.exception.AlreadyPaidException;
import com.delivery.SuAl.exception.GatewayException;
import com.delivery.SuAl.exception.InvalidPaymentStateException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.PaymentCreationException;
import com.delivery.SuAl.exception.PaymentRefundException;
import com.delivery.SuAl.exception.PaymentVerificationException;
import com.delivery.SuAl.mapper.MagnetGatewayMapper;
import com.delivery.SuAl.mapper.PaymentMapper;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.PackageOrderStatus;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.response.payment.CancelRefundResponse;
import com.delivery.SuAl.model.response.payment.CreatePaymentResponse;
import com.delivery.SuAl.model.response.payment.PaymentStatusResponse;
import com.delivery.SuAl.repository.CustomerPackageOrderRepository;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagnetPaymentService implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerPackageOrderRepository customerPackageOrderRepository;
    private final PaymentMapper paymentMapper;
    private final MagnetGatewayMapper gatewayMapper;
    private final RestTemplate magnetRestTemplate;
    private final CampaignService campaignService;
    private final PromoService promoService;

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
    public PaymentDTO initialize(CreatePaymentDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (paymentRepository.hasSuccessfulPayment(order.getId())) {
            throw new AlreadyPaidException("Order already paid");
        }

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
    public PaymentStatus handleCallback(String reference) {
        log.info("=== CALLBACK START === Reference: {}", reference);

        Payment payment = paymentRepository.findByReferenceId(reference)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + reference));
        log.info("Payment found - ID: {}, Current Status: {}, Order ID: {}",
                payment.getId(), payment.getPaymentStatus(),
                payment.getOrder() != null ? payment.getOrder().getId() : "NULL");

        log.info("Fetching external payment status from gateway...");
        PaymentStatusResponse statusResponse = fetchExternalStatus(reference);
        log.info("Gateway response received - Status: {}", statusResponse.getStatus());

        PaymentStatus oldStatus = payment.getPaymentStatus();
        log.info("Updating payment from status response...");
        gatewayMapper.updatePaymentFromStatusResponse(statusResponse, payment);
        log.info("Payment status after mapper: {} (was: {})", payment.getPaymentStatus(), oldStatus);

        paymentRepository.save(payment);
        log.info("Payment saved to database");

        if (payment.getOrder() != null) {
            log.info("Order exists - Order Number: {}, Order Status: {}",
                    payment.getOrder().getOrderNumber(),
                    payment.getOrder().getPaymentStatus());

            if (oldStatus != payment.getPaymentStatus()) {
                log.info("Payment status changed from {} to {}, updating order...",
                        oldStatus, payment.getPaymentStatus());

                updateOrderPaymentStatus(payment.getOrder(), payment);
                log.info("Order payment status updated to: {}", payment.getOrder().getPaymentStatus());

                if (payment.getOrder().getPackageOrder() != null) {
                    log.info("Order belongs to package order, updating package payment status");
                    updatePackageOrderPaymentStatus(
                            payment.getOrder().getPackageOrder(),
                            payment.getPaymentStatus()
                    );
                }

                if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
                    deleteFailedOrder(payment.getOrder());
                } else {
                    log.info("Payment status is {}, saving order...", payment.getPaymentStatus());
                    orderRepository.save(payment.getOrder());
                    log.info("Order saved successfully");
                }
            } else {
                log.info("Payment status unchanged ({}), skipping order update", oldStatus);
            }
        } else {
            log.warn("Payment has no associated order!");
        }

        log.info("=== CALLBACK END === Final Payment Status: {}", payment.getPaymentStatus());
        return payment.getPaymentStatus();
    }

    @Override
    @Transactional
    public void refundPayment(Long orderId) {
        log.info("Attempting refund for order ID: {}", orderId);

        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(orderId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new NotFoundException("No successful payment found for order with ID: " + orderId));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new InvalidPaymentStateException("Payment already refunded");
        }

        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new InvalidPaymentStateException("Payment already cancelled");
        }

        PaymentStatusResponse currentStatus = fetchExternalStatus(payment.getReferenceId());

        if (!"00".equals(currentStatus.getStatus())) {
            throw new InvalidPaymentStateException("Payment not in approved state. Current status: "
                    + currentStatus.getStatus());
        }

        log.info("Payment made on {} - using Refund(post-settlement) for reference: {}",
                payment.getPaidAt().toLocalDate(), payment.getReferenceId());
        refundPayment(payment);

        payment = paymentRepository.save(payment);
        log.info("Refund/Cancel successful for order: {}", orderId);

        paymentMapper.toDto(payment);
    }

    @Override
    @Transactional
    public PaymentDTO checkPaymentStatus(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("No payment found for order: " + orderId));

        PaymentStatusResponse statusResponse = fetchExternalStatus(payment.getReferenceId());
        gatewayMapper.updatePaymentFromStatusResponse(statusResponse, payment);
        return paymentMapper.toDto(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updatePackageOrderPaymentStatus(
            CustomerPackageOrder packageOrder,
            PaymentStatus paymentStatus
    ) {
        log.info("Updating package order {} payment status to {}",
                packageOrder.getId(), paymentStatus);

        packageOrder.setPaymentStatus(paymentStatus);

        if (paymentStatus == PaymentStatus.SUCCESS) {
            log.info("Payment successful - marking all orders in package as PAID");

            for (Order order : packageOrder.getGeneratedOrders()) {
                if (order.getPaymentStatus() == PaymentStatus.PENDING) {
                    order.setPaymentStatus(PaymentStatus.SUCCESS);
                    order.setPaidAt(LocalDateTime.now());
                    orderRepository.save(order);

                    log.debug("Order {} in package marked as PAID", order.getId());
                }
            }

            log.info("Package order {} fully paid - {} orders updated",
                    packageOrder.getId(), packageOrder.getGeneratedOrders().size());

        } else if (paymentStatus == PaymentStatus.FAILED) {
            log.warn("Payment FAILED - cancelling package order and rejecting all orders");

            packageOrder.setOrderStatus(PackageOrderStatus.CANCELLED);
            packageOrder.setCancelledAt(LocalDateTime.now());

            for (Order order : packageOrder.getGeneratedOrders()) {
                if (order.getOrderStatus() == OrderStatus.PENDING) {
                    order.setOrderStatus(OrderStatus.REJECTED);
                    order.setRejectionReason("Package payment failed");
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);

                    log.debug("Order {} in package rejected due to payment failure", order.getId());
                }
            }

            log.info("Package order {} cancelled - all orders rejected", packageOrder.getId());
        }

        customerPackageOrderRepository.save(packageOrder);
        log.info("Package order {} payment status updated successfully", packageOrder.getId());
    }

    private PaymentStatusResponse fetchExternalStatus(String reference) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/payment/status")
                    .queryParam("reference", reference)
                    .toUriString();

            String body = magnetRestTemplate.getForObject(url, String.class);
            return objectMapper.readValue(body, PaymentStatusResponse.class);
        } catch (Exception e) {
            throw new PaymentVerificationException("Could not verify payment with provider", e);
        }
    }

    private void updateOrderPaymentStatus(Order order, Payment payment) {
        PaymentStatus oldStatus = order.getPaymentStatus();
        order.setPaymentMethod(payment.getPaymentMethod());

        switch (payment.getPaymentStatus()) {
            case SUCCESS -> {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setPaidAt(payment.getPaidAt());

                log.info("Order {} payment successful. Amount: {} {}",
                        order.getOrderNumber(), payment.getAmountAsDecimal(), payment.getCurrencyCode());
            }
            case FAILED -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                log.warn("Payment failed for order {}. Reason: {}", order.getOrderNumber(), payment.getFailureReason());
            }

            case PENDING -> {
                order.setPaymentStatus(PaymentStatus.PENDING);
                log.info("Payment still pending for order {}", order.getOrderNumber());
            }

            case REFUNDED -> {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Payment refunded for order {}. Amount: {} coins",
                        order.getOrderNumber(),
                        payment.getRefundAmountInCoins());
            }

            default -> log.warn("Unhandled payment status: {} for order: {}",
                    payment.getPaymentStatus(),
                    order.getOrderNumber());

        }

        if (oldStatus != order.getPaymentStatus()) {
            log.info("Order {} payment status changed: {} -> {}",
                    order.getOrderNumber(), oldStatus, payment.getPaymentStatus());
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

    private void deleteFailedOrder(Order order) {
        log.warn("!!! PAYMENT FAILED - DELETING ORDER !!!");
        log.info("Order to delete - ID: {}, Number: {}", order.getId(), order.getOrderNumber());

        try {
            if (order.getPromo() != null) {
                promoService.releasePromoUsageByOrder(order.getId());
            }

            List<OrderCampaignBonus> campaignBonuses = order.getCampaignBonuses();
            if (campaignBonuses != null && !campaignBonuses.isEmpty()) {
                campaignService.releaseCampaignUsageByOrder(order.getId());
            }

            if (order.getPackageOrder() != null) {
                log.info("Order belongs to package order {}, will be handled by package order cancellation",
                        order.getPackageOrder().getId());
                return;
            }

            orderRepository.delete(order);
            orderRepository.flush();

            log.info("Order {} and all related entities deleted successfully", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to delete order {}: {}", order.getOrderNumber(), e.getMessage(), e);
        }
    }

    private void cancelPayment(Payment payment) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/payment/cancel")
                .queryParam("reference", payment.getReferenceId())
                .queryParam("amount", payment.getAmountInCoins())
                .toUriString();

        try {
            String body = magnetRestTemplate.getForObject(url, String.class);
            CancelRefundResponse response = objectMapper.readValue(body, CancelRefundResponse.class);

            if (response.getCode() != 0) {
                throw new GatewayException("MAGNET cancel failed: " + response.getMessage());
            }

            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            payment.setRefundAmountInCoins(payment.getAmountInCoins());
            payment.setRefundedAt(LocalDateTime.now());

            log.info("Payment cancelled (reversed): {}", payment.getReferenceId());
        } catch (Exception ex) {
            log.error("Cancel request failed", ex);
            throw new PaymentRefundException("Cancel failed: " + ex.getMessage(), ex);
        }
    }

    private void refundPayment(Payment payment) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/payment/refund")
                .queryParam("reference", payment.getReferenceId())
                .queryParam("amount", payment.getAmountInCoins())
                .toUriString();

        try {
            String body = magnetRestTemplate.getForObject(url, String.class);
            CancelRefundResponse response = objectMapper.readValue(body, CancelRefundResponse.class);

            if (response.getCode() != 0) {
                throw new GatewayException("MAGNET refund failed: " + response.getMessage());
            }

            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setRefundAmountInCoins(payment.getAmountInCoins());
            payment.setRefundedAt(LocalDateTime.now());

            log.info("Payment refunded: {}", payment.getReferenceId());
        } catch (Exception ex) {
            log.error("Refund request failed", ex);
            throw new PaymentRefundException("Refund failed: " + ex.getMessage(), ex);
        }
    }
}