package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.exception.InsufficientStockException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {
    private final WarehouseStockRepository warehouseStockRepository;
    private final ProductRepository productRepository;

    public void validateAndReserveStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);
        if (stocks.isEmpty()) {
            throw new NotFoundException("product not found in warehouse with id: " + productId);
        }

        WarehouseStock stock = stocks.getFirst();

        if (stock.getFullCount() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            product.getName(), stock.getFullCount(), quantity)
            );
        }

        stock.setFullCount(stock.getFullCount() - quantity);
        warehouseStockRepository.save(stock);

        log.debug("Reserved {} units of product {}", quantity, product.getName());
    }

    @Transactional
    public void validateAndReserveStockBatch(Map<Long, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty()) {
            log.debug("No products to reserve");
            return;
        }

        log.info("Batch reserving stock for {} products", productQuantities.size());

        List<Long> productIds = new ArrayList<>(productQuantities.keySet());

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> missingProducts = productIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .toList();
        if (!missingProducts.isEmpty()) {
            throw new NotFoundException("Products not found: " + missingProducts);
        }

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductIdsWithLock(productIds);
        Map<Long, WarehouseStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

        List<String> errors = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            WarehouseStock stock = stockMap.get(productId);
            if (stock == null) {
                errors.add("Product " + productId + " not available in warehouse");
                continue;
            }

            if (stock.getFullCount() < quantity) {
                Product product = productMap.get(productId);
                errors.add(String.format("Insufficient stock for %s. Available: %d, Requested: %d",
                        product.getName(), stock.getFullCount(), quantity));
            }
        }

        if (!errors.isEmpty()) {
            throw new InsufficientStockException("Stock validation failed: " + String.join("; ", errors));
        }

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            WarehouseStock stock = stockMap.get(productId);
            stock.setFullCount(stock.getFullCount() - quantity);

            Product product = productMap.get(productId);
            log.debug("Reserved {} units of {} ({}). Remaining: {}",
                    quantity, productId, product.getName(), stock.getFullCount());
        }

        warehouseStockRepository.saveAll(stocks);
        log.info("Successfully reserved stock for {} products", productQuantities.size());
    }



    @Transactional
    public void releaseStock(Long productId, int quantity) {
        log.info("Releasing stock for {} units of product {}", quantity, productId);
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);
        if (!stocks.isEmpty()) {
            WarehouseStock stock = stocks.getFirst();
            stock.setFullCount(stock.getFullCount() + quantity);
            warehouseStockRepository.save(stock);
            log.debug("Released {} units of product ID {}. New count: {}",
                    quantity, productId, stock.getFullCount());
        } else
            log.warn("Cannot release stock - product {} not found in warehouse", productId);
    }

    @Transactional
    public void adjustStock(Long productId, int quantityChange) {
        if (quantityChange > 0) {
            releaseStock(productId, quantityChange);
        } else if (quantityChange < 0) {
            validateAndReserveStock(productId, Math.abs(quantityChange));
        }
    }

    @Transactional
    public void addEmptyBottlesBatch(Map<Long, Integer> emptyBottlesByProduct) {
        if (emptyBottlesByProduct == null || emptyBottlesByProduct.isEmpty()) {
            log.debug("No empty bottles to add");
            return;
        }

        log.info("Batch adding empty bottles for {} products", emptyBottlesByProduct.size());

        List<Long> productIds = new ArrayList<>(emptyBottlesByProduct.keySet());

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductIdsWithLock(productIds);
        Map<Long, WarehouseStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

        int totalBottlesAdded = 0;
        for (Map.Entry<Long, Integer> entry : emptyBottlesByProduct.entrySet()) {
            Long productId = entry.getKey();
            Integer bottleCount = entry.getValue();

            if (bottleCount <= 0) {
                continue;
            }

            WarehouseStock stock = stockMap.get(productId);
            if (stock != null) {
                int previousEmpty = stock.getEmptyCount();
                stock.setEmptyCount(stock.getEmptyCount() + bottleCount);
                totalBottlesAdded += bottleCount;

                log.debug("Added {} empty bottles for product {}. Previous: {}, New: {}",
                        bottleCount, productId, previousEmpty, stock.getEmptyCount());
            } else {
                log.warn("Cannot add empty bottles - product {} not found in warehouse", productId);
            }
        }

        warehouseStockRepository.saveAll(stocks);
        log.info("Successfully added {} total empty bottles across {} products",
                totalBottlesAdded, emptyBottlesByProduct.size());
    }
}