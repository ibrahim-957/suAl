package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.exception.AlreadyPaidException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.PaymentRepository;
import com.delivery.SuAl.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/v1/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${frontend.success-url}")
    private String successUrl;

    @Value("${frontend.failure-url}")
    private String failureUrl;


    @PostMapping("/init")
    public ResponseEntity<ApiResponse<PaymentDTO>> initializePayment(
            @Valid @RequestBody CreatePaymentDTO dto
    ) {

        log.info("Payment initialization requested for orderId={}", dto.getOrderId());

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (paymentRepository.hasSuccessfulPayment(order.getId())) {
            throw new AlreadyPaidException("Order already paid");
        }

        PaymentDTO payment = paymentService.initialize(dto, order);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment));
    }


    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam("reference") String reference) {

        log.info("MAGNET callback received, reference={}", reference);

        try {
            paymentService.handleCallback(reference);

            Payment payment = paymentRepository.findByReferenceId(reference)
                    .orElseThrow(() -> new NotFoundException("Payment not found"));

            String redirect = payment.getPaymentStatus() == PaymentStatus.SUCCESS
                    ? successUrl + "?ref=" + reference
                    : failureUrl + "?ref=" + reference;

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(redirect))
                    .build();

        } catch (Exception ex) {
            log.error("Callback handling failed for reference={}", reference, ex);

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(failureUrl + "?error=callback_failed"))
                    .build();
        }
    }
}