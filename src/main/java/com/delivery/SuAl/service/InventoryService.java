package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {
    private final WarehouseStockRepository warehouseStockRepository;
    private final ProductRepository productRepository;

    public void validateAndReserveStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("product not found"));

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);
        if (stocks.isEmpty()) {
            throw new RuntimeException("product not found");
        }

        WarehouseStock stock = stocks.getFirst();
        if (stock.getFullCount() < quantity) {
            throw new RuntimeException("product full count not enough");
        }

        stock.setFullCount(stock.getFullCount() - quantity);
        warehouseStockRepository.save(stock);

        log.debug("Reserved {} units of product {}", quantity, product.getName());
    }

    public void releaseStock(Long productId, int quantity) {
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);
        if (!stocks.isEmpty()) {
            WarehouseStock stock = stocks.getFirst();
            stock.setFullCount(stock.getFullCount() + quantity);
            warehouseStockRepository.save(stock);
            log.debug("Released {} units of product ID {}", quantity, productId);
        }
    }

    public void adjustStock(Long productId, int quantityChange) {
        if (quantityChange > 0) {
            releaseStock(productId, quantityChange);
        } else if (quantityChange < 0) {
            validateAndReserveStock(productId, Math.abs(quantityChange));
        }
    }

    public void addEmptyBottles(Long productId, int emptyBottlesCount) {
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);
        if (!stocks.isEmpty()) {
            WarehouseStock stock = stocks.getFirst();
            stock.setEmptyCount(stock.getEmptyCount() + emptyBottlesCount);
            warehouseStockRepository.save(stock);
        }
    }
}
