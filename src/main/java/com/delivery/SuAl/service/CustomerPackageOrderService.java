package com.delivery.SuAl.service;

import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.request.affordablepackage.OrderAffordablePackageRequest;
import com.delivery.SuAl.model.response.affordablepackage.CustomerPackageOrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerPackageOrderService {

    CustomerPackageOrderResponse orderPackage(Long customerId, OrderAffordablePackageRequest request);

    PaymentDTO initializePackagePayment(Long packageOrderId, String language);

    void handlePaymentSuccess(Long packageOrderId, String transactionId);

    void handlePaymentFailure(Long packageOrderId, String reason);

    PageResponse<CustomerPackageOrderResponse> getCustomerPackageOrders(Long customerId, Pageable pageable);

    CustomerPackageOrderResponse getPackageOrderById(Long packageOrderId);

    CustomerPackageOrderResponse cancelPackageOrder(Long customerId, Long packageOrderId);

    CustomerPackageOrderResponse toggleAutoRenew(Long customerId, Long packageOrderId, Boolean autoRenew);

    void processAutoRenewals(String previousMonth);

    void updatePackageOrderStatus(Long packageOrderId);
}