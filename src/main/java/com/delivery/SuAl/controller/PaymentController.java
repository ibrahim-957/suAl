package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<PaymentDTO>> initializePayment(
            @Valid @RequestBody CreatePaymentDTO dto
    ) {

        log.info("Payment initialization requested for orderId={}", dto.getOrderId());

        PaymentDTO payment = paymentService.initialize(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment));
    }


    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam("reference") String reference) {
        log.info("MAGNET callback received, reference={}", reference);
        try {
            PaymentStatus status = paymentService.handleCallback(reference);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .build();

        } catch (Exception ex) {
            log.error("Callback processing failed", ex);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .build();
        }
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentDTO> getPaymentStatus(@PathVariable Long orderId) {
        log.info("Checking payment status for order: {}", orderId);
        PaymentDTO payment = paymentService.checkPaymentStatus(orderId);
        return ResponseEntity.ok(payment);
    }
}