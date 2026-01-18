package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;

public interface PaymentService {
    PaymentDTO initialize(CreatePaymentDTO dto, Order order);
    void handleCallback(String reference);
}
