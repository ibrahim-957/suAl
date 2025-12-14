package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.mapper.OrderDetailMapper;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.DiscountType;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentStatus;
import com.delivery.SuAl.model.PromoStatus;
import com.delivery.SuAl.model.request.operation.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.repository.AddressRepository;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.PromoRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final DriverRepository driverRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final PromoRepository promoRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerName());

        Address address = findAddress(request.getAddressId());
        Order order = initializeOrder(request, address);

        List<OrderDetail> orderDetails = buildOrderDetailsFromRequest(request.getItems());
        OrderCalculation calculation = calculateFromOrderDetails(orderDetails);

        BigDecimal totalDepositRefunded = calculateDepositRefund(
                request.getEmptyBottlesExpected(),
                calculation.getDepositPerUnit()
        );

        setOrderTotals(order, calculation, totalDepositRefunded);

        BigDecimal promoDiscount = applyPromoCode(order, request.getPromoCode(), order.getSubtotal());
        calculateFinalAmounts(order, promoDiscount);

        order.setOrderDetails(orderDetails);
        orderDetails.forEach(d -> d.setOrder(order));

        Order savedOrder = orderRepository.save(order);
        updateWarehouseStockForOrder(savedOrder);

        log.info("Order created successfully - Number: {}, Amount: {}",
                savedOrder.getOrderNumber(), savedOrder.getAmount());

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest updateRequest) {
        log.info("Updating order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        boolean needsRecalculation = false;

        if (updateRequest.getNotes() != null) {
            order.setNotes(updateRequest.getNotes());
            log.debug("Updated notes for order: {}", orderId);
        }

        if (updateRequest.getDeliveryDate() != null) {
            order.setDeliveryDate(updateRequest.getDeliveryDate());
            log.debug("Updated delivery date for order: {}", orderId);
        }

        if (updateRequest.getAddressId() != null) {
            Address newAddress = findAddress(updateRequest.getAddressId());
            order.setAddress(newAddress);
            log.debug("Updated address for order: {}", orderId);
        }

        if (updateRequest.getItems() != null && !updateRequest.getItems().isEmpty()) {
            updateOrderItems(order, updateRequest.getItems());
            needsRecalculation = true;
        }

        if (needsRecalculation) {
            recalculateOrderTotalsFromDetails(order);
        }

        if (updateRequest.getEmptyBottlesExpected() != null) {
            updateEmptyBottlesAndRecalculate(order, updateRequest.getEmptyBottlesExpected());
        }

        Order updatedOrder = orderRepository.save(order);

        log.info("Order updated successfully: {}", orderId);
        return orderMapper.toResponse(updatedOrder);
    }


    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Getting order by id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return orderMapper.toResponse(order);
    }


    @Override
    @Transactional
    public void deleteOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        restoreWarehouseStockForOrder(order);

        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public OrderResponse assignDriver(Long orderId, Long driverId) {
        log.info("Assigning driver {} to order {}", driverId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order with id " + orderId + " not found"));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver with id " + driverId + " not found"));

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Order with id " + orderId + " is not approved");
        }

        order.setDriver(driver);
        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order with id " + orderId + " not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING)
            throw new RuntimeException("Order status is not PENDING");

        order.setOrderStatus(OrderStatus.APPROVED);

        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order with id " + orderId + " not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING)
            throw new RuntimeException("Order status is not PENDING");

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);

        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest) {
        log.info("Fetching order with id {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order with id " + orderId + " not found"));

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Order status is not APPROVED");
        }

        if (order.getDriver() == null) {
            throw new RuntimeException("Order has no driver assigned");
        }
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }

        int emptyBottlesCollected = order.getEmptyBottlesCollected();

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setEmptyBottlesCollected(emptyBottlesCollected);
        order.setCompletedAt(LocalDateTime.now());
        order.setPaymentStatus(PaymentStatus.PAID);

        if (completeDeliveryRequest.getNotes() != null && !completeDeliveryRequest.getNotes().trim().isEmpty()) {
            String existingNotes = order.getNotes();
            if (existingNotes != null && !existingNotes.trim().isEmpty()) {
                order.setNotes(existingNotes + " /// " + completeDeliveryRequest.getNotes());
            } else
                order.setNotes(completeDeliveryRequest.getNotes());
        }

        updateWarehouseStockFromCompletedOrder(order, emptyBottlesCollected);

        orderRepository.save(order);

        return orderMapper.toResponse(order);
    }


    @Override
    @Transactional(readOnly = true)
    public Long countTodaysOrders() {
        log.info("Getting Today's orders count");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Long count = orderRepository.countTodaysOrders(startOfDay, endOfDay);

        log.info("Today's orders count: {}", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating revenue from {} to {}", startDate, endDate);

        BigDecimal revenue = orderRepository.calculateRevenue(startDate, endDate);
        BigDecimal result = revenue != null ? revenue : BigDecimal.ZERO;

        log.info("Revenue: {}", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTodaysRevenue() {
        log.info("Calculating today's revenue");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return calculateRevenue(startOfDay, endOfDay);
    }

    private void updateOrderItems(Order order, List<UpdateOrderItemRequest> itemUpdates) {
        log.info("Updating order items for order: {}", order.getOrderNumber());

        Map<Long, OrderDetail> detailMap = order.getOrderDetails().stream()
                .collect(Collectors.toMap(OrderDetail::getId, d -> d));

        for (UpdateOrderItemRequest itemUpdate : itemUpdates) {
            OrderDetail detail = detailMap.get(itemUpdate.getOrderDetailId());

            if (detail == null) {
                log.warn("Order detail not found: {}", itemUpdate.getOrderDetailId());
                continue;
            }

            boolean updated = false;

            if (itemUpdate.getQuantity() != null && !itemUpdate.getQuantity().equals(detail.getCount())) {
                int oldQuantity = detail.getCount();

                int quantityDiff = itemUpdate.getQuantity() - oldQuantity;
                if (quantityDiff > 0) {
                    validateStock(detail.getProduct(), quantityDiff);
                    adjustWarehouseStock(detail.getProduct().getId(), -quantityDiff);
                } else if (quantityDiff < 0) {
                    adjustWarehouseStock(detail.getProduct().getId(), Math.abs(quantityDiff));
                }

                detail.setCount(itemUpdate.getQuantity());
                log.debug("Updated quantity for product {}: {} -> {}",
                        detail.getProduct().getName(), oldQuantity, itemUpdate.getQuantity());
                updated = true;
            }

            if (itemUpdate.getSellPrice() != null &&
                    itemUpdate.getSellPrice().compareTo(detail.getPricePerUnit()) != 0) {
                BigDecimal oldPrice = detail.getPricePerUnit();
                detail.setPricePerUnit(itemUpdate.getSellPrice());
                log.debug("Updated price for product {}: {} -> {}",
                        detail.getProduct().getName(), oldPrice, itemUpdate.getSellPrice());
                updated = true;
            }

            if (updated) {
                recalculateOrderDetail(detail);
            }
        }
    }

    private void adjustWarehouseStock(Long productId, int quantityChange) {
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);

        if (!stocks.isEmpty()) {
            WarehouseStock stock = stocks.getFirst();
            int newCount = stock.getFullCount() + quantityChange;
            stock.setFullCount(Math.max(0, newCount));
            warehouseStockRepository.save(stock);

            log.debug("Adjusted warehouse stock for product ID {}: {} -> {}",
                    productId, stock.getFullCount() - quantityChange, stock.getFullCount());
        }
    }

    private void updateEmptyBottlesAndRecalculate(Order order, Integer emptyBottlesExpected) {
        int oldBottles = order.getEmptyBottlesExpected();
        order.setEmptyBottlesExpected(emptyBottlesExpected);

        BigDecimal depositPerUnit = getDepositPerUnitFromOrder(order);
        BigDecimal newDepositRefunded = calculateDepositRefund(
                emptyBottlesExpected,
                depositPerUnit
        );

        order.setTotalDepositRefunded(newDepositRefunded);

        BigDecimal netDeposit = order.getTotalDepositCharged().subtract(newDepositRefunded);
        order.setNetDeposit(netDeposit);

        BigDecimal finalAmount = order.getTotalAmount().add(netDeposit);
        order.setAmount(finalAmount);

        log.info("Updated empty bottles expected: {} -> {}, New deposit refunded: {}",
                oldBottles, emptyBottlesExpected, newDepositRefunded);
    }

    private void recalculateOrderTotalsFromDetails(Order order) {
        log.debug("Recalculating order totals from details for order: {}", order.getOrderNumber());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        int totalCount = 0;

        for (OrderDetail detail : order.getOrderDetails()) {
            subtotal = subtotal.add(detail.getSubtotal());
            totalDepositCharged = totalDepositCharged.add(detail.getDepositCharged());
            totalCount += detail.getCount();
        }

        order.setSubtotal(subtotal);
        order.setTotalDepositCharged(totalDepositCharged);
        order.setCount(totalCount);

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal totalAmount = subtotal.subtract(promoDiscount);
        order.setTotalAmount(totalAmount);

        BigDecimal totalDepositRefunded = Optional.ofNullable(order.getTotalDepositRefunded())
                .orElse(BigDecimal.ZERO);
        BigDecimal netDeposit = totalDepositCharged.subtract(totalDepositRefunded);
        order.setNetDeposit(netDeposit);


        BigDecimal finalAmount = totalAmount.add(netDeposit);
        order.setAmount(finalAmount);

        log.info("Recalculated totals - Items: {}, Subtotal: {}, Total: {}, Final: {}",
                totalCount, subtotal, totalAmount, finalAmount);
    }

    private BigDecimal getDepositPerUnitFromOrder(Order order) {
        return order.getOrderDetails().stream()
                .map(OrderDetail::getDepositPerUnit)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private Address findAddress(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + id));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    private Price findPrice(Long productId) {
        return priceRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Price not found for product id: " + productId));
    }

    private BigDecimal calculateAmount(BigDecimal price, int qty) {
        return price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDepositRefund(Integer emptyBottlesExpected, BigDecimal depositPerUnit) {
        if (emptyBottlesExpected == null || emptyBottlesExpected <= 0 || depositPerUnit == null) {
            return BigDecimal.ZERO;
        }
        return depositPerUnit.multiply(BigDecimal.valueOf(emptyBottlesExpected))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateStock(Product product, int quantity) {
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(product.getId());
        if (stocks.isEmpty()) {
            throw new RuntimeException("Product not found in warehouse: " + product.getName());
        }

        WarehouseStock stock = stocks.getFirst();

        if (stock.getFullCount() < quantity) {
            throw new RuntimeException(
                    String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                            product.getName(), stock.getFullCount(), quantity)
            );
        }
    }

    private List<OrderDetail> buildOrderDetailsFromRequest(List<OrderItemRequest> items) {
        List<OrderDetail> details = new ArrayList<>();
        for (OrderItemRequest item : items) {
            Product product = findProduct(item.getProductId());
            Price price = findPrice(item.getProductId());

            validateStock(product, item.getQuantity());

            OrderDetail detail = orderDetailMapper.toEntity(item);
            detail.setProduct(product);
            detail.setCompany(product.getCompany());
            detail.setCategory(product.getCategory());
            detail.setPricePerUnit(price.getSellPrice());
            detail.setBuyPrice(price.getBuyPrice());
            detail.setCount(item.getQuantity());
            detail.setDepositPerUnit(product.getDepositAmount());
            detail.setContainersReturned(0);

            recalculateOrderDetail(detail);
            details.add(detail);
        }
        return details;
    }

    private void recalculateOrderDetail(OrderDetail detail) {
        BigDecimal subtotal = calculateAmount(detail.getPricePerUnit(), detail.getCount());
        BigDecimal depositCharged = calculateAmount(detail.getDepositPerUnit(), detail.getCount());

        detail.setSubtotal(subtotal);
        detail.setDepositCharged(depositCharged);
        detail.setDeposit(depositCharged);
        detail.setLineTotal(subtotal.add(depositCharged));
    }

    private OrderCalculation calculateFromOrderDetails(List<OrderDetail> details) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        int totalCount = 0;
        BigDecimal depositPerUnit = null;

        for (OrderDetail detail : details) {
            subtotal = subtotal.add(detail.getSubtotal());
            totalDepositCharged = totalDepositCharged.add(detail.getDepositCharged());
            totalCount += detail.getCount();

            if (depositPerUnit == null) {
                depositPerUnit = detail.getDepositPerUnit();
            }
        }

        return new OrderCalculation(details, subtotal, totalCount, totalDepositCharged, depositPerUnit);
    }

    private Order initializeOrder(CreateOrderRequest request, Address address) {
        Order order = orderMapper.toEntity(request);
        order.setAddress(address);
        order.setOrderNumber(generateOrderNumber());
        order.setEmptyBottlesExpected(
                request.getEmptyBottlesExpected() != null ? request.getEmptyBottlesExpected() : 0
        );

        log.info("Generated order number: {}", order.getOrderNumber());
        return order;
    }

    private void setOrderTotals(Order order, OrderCalculation calculation,
                                BigDecimal totalDepositRefunded) {
        BigDecimal netDeposit = calculation.getTotalDepositCharged()
                .subtract(totalDepositRefunded);

        order.setCount(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(totalDepositRefunded);
        order.setNetDeposit(netDeposit);

        log.info("Order totals - Items: {}, Subtotal: {}, Deposit Charged: {}, " +
                        "Deposit Refunded: {}, Net Deposit: {}",
                calculation.getTotalCount(), calculation.getSubtotal(),
                calculation.getTotalDepositCharged(), totalDepositRefunded, netDeposit);
    }

    private BigDecimal applyPromoCode(Order order, String promoCode, BigDecimal subtotal) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            Promo promo = promoRepository.findByPromoCode(promoCode.trim())
                    .orElseThrow(() -> new RuntimeException("Promo not found: " + promoCode));

            if (!isPromoValid(promo)) {
                log.warn("Promo code is expired or inactive: {}", promoCode);
                return BigDecimal.ZERO;
            }

            BigDecimal promoDiscount = calculatePromoDiscount(promo, subtotal);
            order.setPromo(promo);
            order.setPromoDiscount(promoDiscount);

            log.info("Promo code '{}' applied - Discount: {}", promoCode, promoDiscount);
            return promoDiscount;
        } catch (RuntimeException e) {
            log.warn("Invalid promo code '{}': {}", promoCode, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private boolean isPromoValid(Promo promo) {
        LocalDate now = LocalDate.now();
        if (promo.getPromoStatus() != PromoStatus.ACTIVE) {
            return false;
        }
        return promo.getValidFrom().isBefore(now) && promo.getValidTo().isAfter(now);
    }

    private BigDecimal calculatePromoDiscount(Promo promo, BigDecimal subtotal) {
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal
                    .multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = promo.getDiscountValue();
        }

        if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
            discount = promo.getMaxDiscount();
        }
        return discount;
    }

    private void calculateFinalAmounts(Order order, BigDecimal promoDiscount) {
        BigDecimal totalAmount = order.getSubtotal().subtract(promoDiscount);
        BigDecimal finalAmount = totalAmount.add(order.getNetDeposit());

        order.setTotalAmount(totalAmount);
        order.setAmount(finalAmount);

        log.info("Final order amount: {} (Total: {} + Net Deposit: {})",
                finalAmount, totalAmount, order.getNetDeposit());
    }

    private void updateWarehouseStockForOrder(Order order) {
        log.debug("Updating warehouse stock for order: {}", order.getOrderNumber());
        for (OrderDetail detail : order.getOrderDetails()) {
            List<WarehouseStock> stocks =
                    warehouseStockRepository.findByProductId(detail.getProduct().getId());

            if (!stocks.isEmpty()) {
                WarehouseStock stock = stocks.getFirst();
                int newFullCount = stock.getFullCount() - detail.getCount();

                stock.setFullCount(Math.max(0, newFullCount));

                warehouseStockRepository.save(stock);
                log.debug("Stock updated for product {}: Full {} -> {}",
                        detail.getProduct().getName(),
                        stock.getFullCount() + detail.getCount(), stock.getFullCount());
            }
        }
    }

    private synchronized String generateOrderNumber() {
        String prefix = "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<String> existingNumbers = orderRepository.findOrderNumbersByPrefix(prefix);

        int maxSequence = existingNumbers.stream()
                .map(orderNum -> {
                    try {
                        String numberPart = orderNum.substring(orderNum.lastIndexOf('-') + 1);
                        return Integer.parseInt(numberPart);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0);

        return String.format("%s-%04d", prefix, maxSequence + 1);
    }

    private void restoreWarehouseStockForOrder(Order order) {
        log.debug("Restoring warehouse stock for order: {}", order.getOrderNumber());
        for (OrderDetail detail : order.getOrderDetails()) {
            List<WarehouseStock> stocks =
                    warehouseStockRepository.findByProductId(detail.getProduct().getId());

            if (!stocks.isEmpty()) {
                WarehouseStock stock = stocks.getFirst();
                stock.setFullCount(stock.getFullCount() + detail.getCount());

                warehouseStockRepository.save(stock);

                log.debug("Stock restored for product {}: Full {} -> {}",
                        detail.getProduct().getName(),
                        stock.getFullCount() - detail.getCount(), stock.getFullCount());
            }
        }
    }

    private void updateWarehouseStockFromCompletedOrder(Order order, int emptyBottlesCollected) {
        OrderDetail detail = order.getOrderDetails().getFirst();
        Product product = detail.getProduct();

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(product.getId());

        WarehouseStock stock = stocks.getFirst();

        stock.setEmptyCount(stock.getEmptyCount() + emptyBottlesCollected);

        warehouseStockRepository.save(stock);
    }

    private static class OrderCalculation {
        private final List<OrderDetail> orderDetails;
        private final BigDecimal subtotal;
        private final int totalCount;
        private final BigDecimal totalDepositCharged;
        private final BigDecimal depositPerUnit;

        public OrderCalculation(List<OrderDetail> orderDetails, BigDecimal subtotal,
                                int totalCount, BigDecimal totalDepositCharged,
                                BigDecimal depositPerUnit) {
            this.orderDetails = orderDetails;
            this.subtotal = subtotal;
            this.totalCount = totalCount;
            this.totalDepositCharged = totalDepositCharged;
            this.depositPerUnit = depositPerUnit;
        }

        public List<OrderDetail> getOrderDetails() { return orderDetails; }
        public BigDecimal getSubtotal() { return subtotal; }
        public int getTotalCount() { return totalCount; }
        public BigDecimal getTotalDepositCharged() { return totalDepositCharged; }
        public BigDecimal getDepositPerUnit() { return depositPerUnit; }
    }

}


