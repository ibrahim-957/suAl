package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.purchase.CreatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.request.purchase.UpdatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.response.purchase.PurchaseInvoiceResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.PurchaseInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/purchase-invoices")
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService purchaseInvoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseInvoiceResponse>> createInvoice(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePurchaseInvoiceRequest request) {
        log.info("POST /v1/api/purchase-invoices - by user: {}", user.getEmail());

        PurchaseInvoiceResponse response = purchaseInvoiceService.createInvoice(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase invoice created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseInvoiceResponse>> getInvoiceById(
            @PathVariable Long id) {
        log.info("GET /v1/api/purchase-invoices/{}", id);

        PurchaseInvoiceResponse response = purchaseInvoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseInvoiceResponse>> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseInvoiceRequest request) {
        log.info("PUT /v1/api/purchase-invoices/{}", id);

        PurchaseInvoiceResponse response = purchaseInvoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Purchase invoice updated successfully", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseInvoiceResponse>> approveInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        log.info("POST /v1/api/purchase-invoices/{}/approve - by user: {}", id, user.getEmail());

        PurchaseInvoiceResponse response = purchaseInvoiceService.approveInvoice(id, user);
        return ResponseEntity.ok(ApiResponse.success("Purchase invoice approved successfully", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseInvoiceResponse>> cancelInvoice(
            @PathVariable Long id) {
        log.info("POST /v1/api/purchase-invoices/{}/cancel", id);

        PurchaseInvoiceResponse response = purchaseInvoiceService.cancelInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase invoice cancelled successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PurchaseInvoiceResponse>>> getAllInvoices(
            @ParameterObject Pageable pageable) {
        log.info("GET /v1/api/purchase-invoices");

        PageResponse<PurchaseInvoiceResponse> response = purchaseInvoiceService.getAllInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}