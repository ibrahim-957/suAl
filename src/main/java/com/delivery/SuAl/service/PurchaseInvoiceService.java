package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.purchase.CreatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.request.purchase.UpdatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.response.purchase.PurchaseInvoiceResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PurchaseInvoiceService {
    PurchaseInvoiceResponse createInvoice(CreatePurchaseInvoiceRequest request, User createdBy);

    PurchaseInvoiceResponse getInvoiceById(Long id);

    PurchaseInvoiceResponse updateInvoice(Long id, UpdatePurchaseInvoiceRequest request);

    PurchaseInvoiceResponse approveInvoice(Long id, User approvedBy);

    PurchaseInvoiceResponse cancelInvoice(Long id);

    PageResponse<PurchaseInvoiceResponse> getAllInvoices(Pageable pageable);
}
