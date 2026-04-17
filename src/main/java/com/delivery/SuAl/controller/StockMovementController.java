package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.enums.ReferenceType;
import com.delivery.SuAl.model.response.inventory.StockMovementResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/c1/api/stock-movements")
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class StockMovementController {
    private final StockMovementService stockMovementService;

    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getMovementsForProduct(
            @PathVariable Long productId, @PathVariable Long warehouseId){
        log.info("GET /v1/api/stock-movements/product/{}/warehouse/{}", productId, warehouseId);

        List<StockMovementResponse> responses =
                stockMovementService.getMovementsForProduct(productId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getMovementsForReference(
            @PathVariable Long referenceId,
            @RequestParam ReferenceType referenceType) {
        log.info("GET /v1/api/stock-movements/reference/{} type: {}", referenceId, referenceType);

        List<StockMovementResponse> response =
                stockMovementService.getMovementsForReference(referenceType, referenceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}/warehouse/{warehouseId}/net-stock")
    public ResponseEntity<ApiResponse<Integer>> getNetStock(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        log.info("GET /v1/api/stock-movements/product/{}/warehouse/{}/net-stock",
                productId, warehouseId);

        Integer netStock = stockMovementService.getNetStock(productId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success("Net stock calculated", netStock));
    }
}
