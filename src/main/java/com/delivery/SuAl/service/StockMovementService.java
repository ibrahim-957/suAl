package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.StockMovement;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.StockMovementMapper;
import com.delivery.SuAl.model.enums.MovementType;
import com.delivery.SuAl.model.enums.ReferenceType;
import com.delivery.SuAl.model.response.inventory.StockMovementResponse;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.StockMovementRepository;
import com.delivery.SuAl.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockMovementService {
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementMapper stockMovementMapper;

    @Transactional
    public StockMovement record(Long productId, Long warehouseId, MovementType movementType,
                                ReferenceType referenceType, Long referenceId,
                                Integer quantity, String notes){
        if (quantity == null || quantity <= 0){
            throw new IllegalArgumentException(
                    "StockMovement quantity must be positive. Got: " + quantity
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + warehouseId));

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setMovementType(movementType);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setQuantity(quantity);
        movement.setNotes(notes);

        StockMovement saved =  stockMovementRepository.save(movement);

        log.debug("StockMovement recorded: {} {} units of product {} in warehouse {} (ref: {} {})",
                movementType, quantity, productId, warehouseId, referenceType, referenceId);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsForProduct(Long productId, Long warehouseId){
        log.info("Getting stock movements for product {} in warehouse {}", productId, warehouseId);
        return stockMovementMapper.toResponseList(
                stockMovementRepository
                        .findByProductIdAndWarehouseIdOrderByCreatedAtDesc(productId, warehouseId));
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsForReference(ReferenceType referenceType, Long referenceId){
        log.info("Getting stock movements for {} ID: {}", referenceType, referenceId);
        return stockMovementMapper.toResponseList(
                stockMovementRepository
                        .findByReferenceTypeAndReferenceId(referenceType, referenceId));
    }

    @Transactional(readOnly = true)
    public Integer getNetStock(Long productId, Long warehouseId){
        return stockMovementRepository.calculateNetStock(productId, warehouseId);
    }
}
