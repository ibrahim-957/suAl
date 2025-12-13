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
import com.delivery.SuAl.model.DriverStatus;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentStatus;
import com.delivery.SuAl.model.request.operation.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderStatusRequest;
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
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        log.info("Creating new order for customer: {}", createOrderRequest.getCustomerName());

        Address address = findAddress(createOrderRequest.getAddressId());
        Order order = initializeOrder(createOrderRequest, address);

        OrderCalculation calculation = processOrderItems(createOrderRequest.getItems(), order);

        BigDecimal totalDepositRefunded = calculateDepositRefund(
                createOrderRequest.getEmptyBottlesExpected(),
                calculation.getDepositPerUnit()
        );

        setOrderTotals(order, calculation, totalDepositRefunded);

        BigDecimal promoDiscount = applyPromoCode(order, createOrderRequest.getPromoCode(), order.getSubtotal());

        calculateFinalAmounts(order, promoDiscount);

        Order savedOrder = saveOrderWithDetails(order, calculation.getOrderDetails());

        updateWarehouseStockForOrder(savedOrder);

        log.info("Order created successfully - Number: {}, Amount: {}",
                savedOrder.getOrderNumber(), savedOrder.getAmount());

        return orderMapper.toResponse(savedOrder);
    }

    private Address findAddress(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }

    private Order initializeOrder(CreateOrderRequest request, Address address) {
        Order order = orderMapper.toEntity(request);
        order.setAddress(address);
        order.setOrderNumber(generateOrderNumber());
        order.setEmptyBottlesExpected(request.getEmptyBottlesExpected() != null ? request.getEmptyBottlesExpected() : 0);

        log.info("Generated order number: {}", order.getOrderNumber());
        return order;
    }

    private OrderCalculation processOrderItems(List<OrderItemRequest> items, Order order) {
        List<OrderDetail> orderDetails = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalCount = 0;
        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        BigDecimal depositPerUnit = null;

        for (OrderItemRequest itemRequest : items) {
            OrderDetail orderDetail = processOrderItem(itemRequest);

            orderDetails.add(orderDetail);
            subtotal = subtotal.add(orderDetail.getSubtotal());
            totalCount += itemRequest.getQuantity();
            totalDepositCharged = totalDepositCharged.add(orderDetail.getDepositCharged());
            depositPerUnit = orderDetail.getDepositPerUnit();
        }

        return new OrderCalculation(orderDetails, subtotal, totalCount, totalDepositCharged, depositPerUnit);
    }

    private OrderDetail processOrderItem(OrderItemRequest itemRequest) {
        Product product = findProduct(itemRequest.getProductId());
        Price price = findPrice(itemRequest.getProductId());

        validateStock(product, itemRequest);

        log.debug("Processing item - Product: {}, Quantity: {}, Price: {}",
                product.getName(), itemRequest.getQuantity(), price.getSellPrice());

        OrderDetail orderDetail = orderDetailMapper.toEntity(itemRequest);
        orderDetail.setProduct(product);
        orderDetail.setCompany(product.getCompany());
        orderDetail.setCategory(product.getCategory());
        orderDetail.setPricePerUnit(price.getSellPrice());
        orderDetail.setBuyPrice(price.getBuyPrice());
        orderDetail.setCount(itemRequest.getQuantity());

        BigDecimal itemSubtotal = calculateItemSubtotal(price.getSellPrice(), itemRequest.getQuantity());
        orderDetail.setSubtotal(itemSubtotal);

        BigDecimal depositPerUnit = product.getDepositAmount();
        orderDetail.setDepositPerUnit(depositPerUnit);

        BigDecimal depositCharged = calculateDeposit(depositPerUnit, itemRequest.getQuantity());
        orderDetail.setDepositCharged(depositCharged);
        orderDetail.setDeposit(depositCharged);
        orderDetail.setContainersReturned(0);

        BigDecimal lineTotal = itemSubtotal.add(depositCharged);
        orderDetail.setLineTotal(lineTotal);

        return orderDetail;
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    private Price findPrice(Long productId) {
        return priceRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Price not found"));
    }

    private void validateStock(Product product, OrderItemRequest itemRequest) {
        WarehouseStock warehouseStock = warehouseStockRepository.findByProductId(product.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in warehouse"));

        if (warehouseStock.getFullCount() < itemRequest.getQuantity()) {
            throw new RuntimeException(
                    String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                            product.getName(), warehouseStock.getFullCount(), itemRequest.getQuantity())
            );
        }
    }

    private BigDecimal calculateItemSubtotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDeposit(BigDecimal depositPerUnit, int quantity) {
        return depositPerUnit.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDepositRefund(Integer emptyBottlesExpected, BigDecimal depositPerUnit) {
        if (emptyBottlesExpected == null || emptyBottlesExpected <= 0 || depositPerUnit == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal refund = depositPerUnit
                .multiply(BigDecimal.valueOf(emptyBottlesExpected))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Total deposit refunded: {}", refund);
        return refund;
    }

    private void setOrderTotals(Order order, OrderCalculation calculation, BigDecimal totalDepositRefunded) {
        BigDecimal netDeposit = calculation.getTotalDepositCharged().subtract(totalDepositRefunded);

        order.setCount(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(totalDepositRefunded);
        order.setNetDeposit(netDeposit);

        log.info("Order totals - Items: {}, Subtotal: {}, Deposit Charged: {}, Deposit Refunded: {}, Net Deposit: {}",
                calculation.getTotalCount(), calculation.getSubtotal(),
                calculation.getTotalDepositCharged(), totalDepositRefunded, netDeposit);
    }

    private BigDecimal applyPromoCode(Order order, String promoCode, BigDecimal subtotal) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            Promo promo = promoRepository.findByPromoCode(promoCode)
                    .orElseThrow(() -> new RuntimeException("Promo not found"));

            BigDecimal promoDiscount = calculatePromoDiscount(promo, subtotal);
            order.setPromo(promo);
            order.setPromoDiscount(promoDiscount);

            log.info("Promo code '{}' applied - Discount: {}", promoCode, promoDiscount);
            return promoDiscount;
        } catch (RuntimeException e) {
            log.warn("Invalid promo code: {}", promoCode);
            return BigDecimal.ZERO;
        }
    }

    private void calculateFinalAmounts(Order order, BigDecimal promoDiscount) {
        BigDecimal totalAmount = order.getSubtotal().subtract(promoDiscount);
        BigDecimal finalAmount = totalAmount.add(order.getNetDeposit());

        order.setTotalAmount(totalAmount);
        order.setAmount(finalAmount);

        log.info("Final order amount: {} (Total: {} + Net Deposit: {})",
                finalAmount, totalAmount, order.getNetDeposit());
    }

    private Order saveOrderWithDetails(Order order, List<OrderDetail> orderDetails) {
        Order savedOrder = orderRepository.save(order);

        for (OrderDetail orderDetail : orderDetails) {
            orderDetail.setOrder(savedOrder);
        }

        savedOrder.setOrderDetails(orderDetails);
        return orderRepository.save(savedOrder);
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

        if (order.getOrderStatus() != OrderStatus.APPROVED){
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

    private String generateOrderNumber() {
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = orderRepository.countByOrderNumberStartingWith(prefix);
        return String.format("/%s-%04d", prefix, count + 1);
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

        if (promo.getMaxDiscount() != null && subtotal.compareTo(promo.getMaxDiscount()) > 0) {
            discount = promo.getMaxDiscount();
        }
        return discount;
    }

    private void updateWarehouseStockForOrder(Order order) {
        log.debug("Updating warehouse stock for order: {}", order.getOrderNumber());
        for (OrderDetail detail : order.getOrderDetails()) {
            List<WarehouseStock> stocks =
                    warehouseStockRepository.findByProductId(detail.getProduct().getId());

            if (!stocks.isEmpty()) {
                WarehouseStock stock = stocks.get(0);
                int newFullCount = stock.getFullCount() - detail.getCount();

                stock.setFullCount(Math.max(0, newFullCount));

                warehouseStockRepository.save(stock);
                log.debug("Stock updated for product {}: Full {} -> {}",
                        detail.getProduct().getName(),
                        stock.getFullCount() + detail.getCount(), stock.getFullCount());
            }
        }
    }

    private void restoreWarehouseStockForOrder(Order order) {
        log.debug("Restoring warehouse stock for order: {}", order.getOrderNumber());
        for (OrderDetail detail : order.getOrderDetails()) {
            List<WarehouseStock> stocks =
                    warehouseStockRepository.findByProductId(detail.getProduct().getId());

            if (!stocks.isEmpty()) {
                WarehouseStock stock = stocks.get(0);
                stock.setFullCount(stock.getFullCount() + detail.getCount());

                warehouseStockRepository.save(stock);

                log.debug("Stock restored for product {}: Full {} -> {}",
                        detail.getProduct().getName(),
                        stock.getFullCount() - detail.getCount(), stock.getFullCount());
            }
        }
    }

    private void updateWarehouseStockFromCompletedOrder(Order order, int emptyBottlesCollected) {
        OrderDetail detail = order.getOrderDetails().get(0);
        Product product = detail.getProduct();

        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(product.getId());

        WarehouseStock stock = stocks.get(0);

        int currentEmptyCount = stock.getEmptyCount();
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


