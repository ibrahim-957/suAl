package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.StockMovement;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.exception.InsufficientStockException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.model.enums.MovementType;
import com.delivery.SuAl.model.enums.ReferenceType;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.StockMovementRepository;
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
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public void validateAndReserveStock(Long productId, int quantity) {
        log.info("Reserving {} units of product {}", quantity, productId);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        WarehouseStock stock = warehouseStockRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("product not found in warehouse with id: " + productId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        if (stock.getFullCount() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            product.getName(), stock.getFullCount(), quantity)
            );
        }

        stock.setFullCount(stock.getFullCount() - quantity);
        warehouseStockRepository.save(stock);

        log.info("Reserved {} units of product '{}'. Remaining: {}",
                quantity, product.getName(), stock.getFullCount());
    }

    @Transactional
    public void validateAndReserveStockBatch(Map<Long, Integer> productQuantities, User user) {
        validateAndReserveStockBatch(productQuantities, null, null, user);
    }

    @Transactional
    public void validateAndReserveStockBatch(Map<Long, Integer> productQuantities,
                                             Long orderId,
                                             Long warehouseId,
                                             User user) {
        if (productQuantities == null || productQuantities.isEmpty()) return;
        log.info("Batch reserving stock for {} products", productQuantities.size());

        productQuantities.forEach((productId, quantity) -> {
            if (quantity <= 0) throw new IllegalArgumentException(
                    String.format("Invalid quantity %d for product %d", quantity, productId));
        });

        List<Long> productIds = new ArrayList<>(productQuantities.keySet());
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductIdsWithLock(productIds);
        Map<Long, WarehouseStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> missingProducts = productIds.stream()
                .filter(id -> !productMap.containsKey(id)).toList();
        if (!missingProducts.isEmpty()) {
            throw new NotFoundException("Products not found: " + missingProducts);
        }

        List<String> errors = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            WarehouseStock stock = stockMap.get(productId);
            if (stock == null) {
                errors.add(String.format("Product '%s' not available in warehouse",
                        productMap.get(productId).getName()));
                continue;
            }
            if (stock.getFullCount() < quantity) {
                errors.add(String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                        productMap.get(productId).getName(), stock.getFullCount(), quantity));
            }
        }
        if (!errors.isEmpty()) {
            throw new InsufficientStockException("Stock validation failed: " + String.join("; ", errors));
        }

        List<StockMovement> movements = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            WarehouseStock stock = stockMap.get(productId);
            stock.setFullCount(stock.getFullCount() - quantity);

            if (orderId != null && warehouseId != null) {
                StockMovement movement = new StockMovement();
                movement.setProduct(productMap.get(productId));
                movement.setWarehouse(stock.getWarehouse());
                movement.setMovementType(MovementType.SALE);
                movement.setReferenceType(ReferenceType.ORDER);
                movement.setReferenceId(orderId);
                movement.setQuantity(quantity);
                movement.setNotes("Stock reserved for order ID: " + orderId);
                movement.setCreatedBy(user);
                movements.add(movement);
            }

            log.debug("Reserved {} of {} ({}). Remaining: {}",
                    quantity, productId, productMap.get(productId).getName(), stock.getFullCount());
        }

        warehouseStockRepository.saveAll(stocks);
        if (!movements.isEmpty()) {
            stockMovementRepository.saveAll(movements);
        }

        log.info("Successfully reserved stock for {} products", productQuantities.size());
    }

    @Transactional
    public void softReserveStockBatch(Map<Long, Integer> productQuantities) {
        log.info("SOFT reserving stock for {} products", productQuantities.size());

        validateStockAvailability(productQuantities);
    }

    @Transactional
    public void convertSoftToHardReservation(Map<Long, Integer> productQuantities, User user) {
        log.info("Converting SOFT to HARD reservation for {} products", productQuantities.size());

        validateAndReserveStockBatch(productQuantities, user);
    }

    @Transactional
    public void releaseStock(Long productId, int quantity) {
        if (quantity <= 0) {
            log.warn("Attempted to release non-positive quantity {} for product {}", quantity, productId);
            return;
        }

        log.info("Releasing stock for {} units of product {} back to warehouse", quantity, productId);

        warehouseStockRepository.findByProductIdWithLock(productId)
                .ifPresentOrElse(
                        stock -> {
                            stock.setFullCount(stock.getFullCount() + quantity);
                            warehouseStockRepository.save(stock);
                            log.info("Released {} units of product ID {}. New count: {}",
                                    quantity, productId, stock.getFullCount());
                        },
                        () -> {
                            log.error("Cannot release stock - product {} not found in warehouse", productId);
                            throw new NotFoundException(
                                    "Product not found in warehouse with id: " + productId);
                        }
                );
    }

    @Transactional
    public void releaseStockBatch(Map<Long, Integer> productQuantities) {
        releaseStockBatch(productQuantities, null, null);
    }

    @Transactional
    public void releaseStockBatch(Map<Long, Integer> productQuantities,
                                  Long orderId,
                                  Long warehouseId) {
        if (productQuantities == null || productQuantities.isEmpty()) return;
        log.info("Batch releasing stock for {} products", productQuantities.size());

        productQuantities.forEach((productId, quantity) -> {
            if (quantity <= 0) throw new IllegalArgumentException(
                    String.format("Invalid quantity %d for product %d", quantity, productId));
        });

        List<Long> productIds = new ArrayList<>(productQuantities.keySet());
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductIdsWithLock(productIds);
        Map<Long, WarehouseStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> missingStocks = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            WarehouseStock stock = stockMap.get(productId);
            if (stock == null) {
                missingStocks.add(productId);
                log.error("Cannot release stock - product {} not found in warehouse", productId);
                continue;
            }

            stock.setFullCount(stock.getFullCount() + quantity);

            if (orderId != null) {
                StockMovement movement = new StockMovement();
                movement.setProduct(productMap.get(productId));
                movement.setWarehouse(stock.getWarehouse());
                movement.setMovementType(MovementType.RETURN_FROM_CUSTOMER);
                movement.setReferenceType(ReferenceType.ORDER);
                movement.setReferenceId(orderId);
                movement.setQuantity(quantity);
                movement.setNotes("Stock released from cancelled order ID: " + orderId);
                movements.add(movement);
            }

            log.debug("Released {} of {} ({}). New count: {}",
                    quantity, productId,
                    productMap.get(productId) != null ? productMap.get(productId).getName() : "Unknown",
                    stock.getFullCount());
        }

        if (!missingStocks.isEmpty()) {
            throw new NotFoundException("Products not found in warehouse: " + missingStocks);
        }

        warehouseStockRepository.saveAll(stocks);
        if (!movements.isEmpty()) {
            stockMovementRepository.saveAll(movements);
        }

        log.info("Successfully released stock for {} products", productQuantities.size());
    }

    @Transactional
    public void adjustStock(Long productId, int quantityChange) {
        if (quantityChange == 0) {
            log.debug("No stock adjustment needed for product {}", productId);
            return;
        }

        if (quantityChange > 0) {
            releaseStock(productId, quantityChange);
        } else {
            validateAndReserveStock(productId, Math.abs(quantityChange));
        }
    }

    @Transactional
    public void addEmptyBottlesBatch(Map<Long, Integer> emptyBottlesByProduct) {
        addEmptyBottlesBatch(emptyBottlesByProduct, null, null);
    }

    @Transactional
    public void addEmptyBottlesBatch(Map<Long, Integer> emptyBottlesByProduct,
                                     Long orderId,
                                     Long warehouseId) {
        if (emptyBottlesByProduct == null || emptyBottlesByProduct.isEmpty()) return;
        log.info("Batch adding empty bottles for {} products", emptyBottlesByProduct.size());

        List<Long> productIds = new ArrayList<>(emptyBottlesByProduct.keySet());
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductIdsWithLock(productIds);
        Map<Long, WarehouseStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<StockMovement> movements = new ArrayList<>();
        int totalBottlesAdded = 0;

        for (Map.Entry<Long, Integer> entry : emptyBottlesByProduct.entrySet()) {
            Long productId = entry.getKey();
            Integer bottleCount = entry.getValue();

            if (bottleCount <= 0) continue;

            WarehouseStock stock = stockMap.get(productId);
            if (stock != null) {
                long newCount = (long) stock.getEmptyCount() + bottleCount;
                if (newCount > Integer.MAX_VALUE) {
                    throw new IllegalStateException(
                            "Empty bottle count overflow for product: " + productId);
                }
                stock.setEmptyCount((int) newCount);
                totalBottlesAdded += bottleCount;

                if (orderId != null) {
                    StockMovement movement = new StockMovement();
                    movement.setProduct(productMap.get(productId));
                    movement.setWarehouse(stock.getWarehouse());
                    movement.setMovementType(MovementType.RETURN_FROM_CUSTOMER);
                    movement.setReferenceType(ReferenceType.ORDER);
                    movement.setReferenceId(orderId);
                    movement.setQuantity(bottleCount);
                    movement.setNotes("Empty containers collected from order ID: " + orderId);
                    movements.add(movement);
                }

                log.debug("Added {} empty bottles for product {}. New empty count: {}",
                        bottleCount, productId, stock.getEmptyCount());
            } else {
                log.warn("Cannot add empty bottles — product {} not found in warehouse", productId);
            }
        }

        warehouseStockRepository.saveAll(stocks);
        if (!movements.isEmpty()) {
            stockMovementRepository.saveAll(movements);
        }

        log.info("Added {} total empty bottles across {} products",
                totalBottlesAdded, emptyBottlesByProduct.size());
    }

    @Transactional
    public void validateStockAvailability(Map<Long, Integer> productQuantities){
        if (productQuantities == null || productQuantities.isEmpty()) {
            return;
        }

        log.debug("Validating stock availability for {} products", productQuantities.size());

        List<Long> productIds = new ArrayList<>(productQuantities.keySet());

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductIdsWithLock(productIds);

        Map<Long, WarehouseStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<String> errors = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            WarehouseStock stock = stockMap.get(productId);
            if (stock == null) {
                Product product = productMap.get(productId);
                errors.add(String.format("Product '%s' not available in warehouse",
                        product != null ? product.getName() : productId));
                continue;
            }

            if (stock.getFullCount() < quantity) {
                Product product = productMap.get(productId);
                errors.add(String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                        product != null ? product.getName() : productId,
                        stock.getFullCount(), quantity));
            }
        }

        if (!errors.isEmpty()) {
            throw new InsufficientStockException("Stock validation failed: " + String.join("; ", errors));
        }
    }
}