package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.enums.PaymentStatus;

public interface PaymentService {
    PaymentDTO initialize(CreatePaymentDTO dto);
    PaymentStatus handleCallback(String reference);
    PaymentDTO refundPayment(Long orderId);
    PaymentDTO checkPaymentStatus(Long orderId);
}
