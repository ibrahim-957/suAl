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

        Address address = addressRepository.findById(createOrderRequest.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Order order = orderMapper.toEntity(createOrderRequest);
        order.setAddress(address);
        order.setOrderNumber(generateOrderNumber());
        log.info("Generated order number: {}", order.getOrderNumber());

        int emptyBottlesExpected = createOrderRequest.getEmptyBottlesExpected() != null ?
                createOrderRequest.getEmptyBottlesExpected() : 0;

        order.setEmptyBottlesExpected(emptyBottlesExpected);

        List<OrderDetail> orderDetails = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalCount = 0;
        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        BigDecimal depositPerUnit = null;

        for (OrderItemRequest itemRequest : createOrderRequest.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            Price price = priceRepository.findByProductId(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Price not found"));

            WarehouseStock warehouseStock = warehouseStockRepository.findByProductId(itemRequest.getProductId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Product not found in warehouse"));


            if (warehouseStock.getFullCount() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                        String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                                product.getName(), warehouseStock.getFullCount(), itemRequest.getQuantity()));
            }

            log.debug("Processing item - Product: {}, Quantity: {}, Price: {}",
                    product.getName(), itemRequest.getQuantity(), price.getSellPrice());

            OrderDetail orderDetail = orderDetailMapper.toEntity(itemRequest);
            orderDetail.setProduct(product);
            orderDetail.setCompany(product.getCompany());
            orderDetail.setCategory(product.getCategory());
            orderDetail.setPricePerUnit(price.getSellPrice());
            orderDetail.setBuyPrice(price.getBuyPrice());
            orderDetail.setCount(itemRequest.getQuantity());

            BigDecimal itemSubtotal = price.getSellPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            orderDetail.setSubtotal(itemSubtotal);

            depositPerUnit = product.getDepositAmount();
            orderDetail.setDepositPerUnit(depositPerUnit);

            BigDecimal depositCharged = depositPerUnit
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            orderDetail.setDepositCharged(depositCharged);

            orderDetail.setContainersReturned(0);

            orderDetail.setDeposit(depositCharged);

            BigDecimal lineTotal = itemSubtotal.add(depositCharged);
            orderDetail.setLineTotal(lineTotal);

            orderDetails.add(orderDetail);

            subtotal = subtotal.add(itemSubtotal);
            totalCount += itemRequest.getQuantity();
            totalDepositCharged = totalDepositCharged.add(depositCharged);
        }

        BigDecimal totalDepositRefunded = BigDecimal.ZERO;

        if (emptyBottlesExpected > 0 && depositPerUnit != null) {
            totalDepositRefunded = depositPerUnit
                    .multiply(BigDecimal.valueOf(emptyBottlesExpected))
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Total deposit refunded: {}", totalDepositRefunded);
        }

        BigDecimal netDeposit = totalDepositCharged.subtract(totalDepositRefunded);

        order.setCount(totalCount);
        order.setSubtotal(subtotal);
        order.setTotalDepositCharged(totalDepositCharged);
        order.setTotalDepositRefunded(totalDepositRefunded);
        order.setNetDeposit(netDeposit);

        log.info("Order totals - Items: {}, Subtotal: {}, Deposit Charged: {}, Deposit Refunded: {}, Net Deposit: {}",
                totalCount, subtotal, totalDepositCharged, totalDepositRefunded, netDeposit);

        BigDecimal promoDiscount = BigDecimal.ZERO;
        if (createOrderRequest.getPromoCode() != null &&
                createOrderRequest.getPromoCode().trim().isEmpty()) {
            try {
                Promo promo = promoRepository.findByPromoCode(createOrderRequest.getPromoCode())
                        .orElseThrow(() -> new RuntimeException("Promo not found"));

                promoDiscount = calculatePromoDiscount(promo, subtotal);
                order.setPromo(promo);
                order.setPromoDiscount(promoDiscount);

                log.info("Promo code'{}' applied - Discount: {}", createOrderRequest.getPromoCode(), promoDiscount);
            } catch (RuntimeException e) {
                log.warn("Invalid promo code: {}", createOrderRequest.getPromoCode());
            }
        }

        BigDecimal totalAmount = subtotal.subtract(promoDiscount);
        BigDecimal finalAmount = totalAmount.add(netDeposit);

        order.setTotalAmount(totalAmount);
        order.setAmount(finalAmount);

        log.info("Final order amount: {} (Total: {} + Net Deposit: {})",
                finalAmount, totalAmount, netDeposit);

        Order savedOrder = orderRepository.save(order);

        for (OrderDetail orderDetail : orderDetails) {
            orderDetail.setOrder(savedOrder);
        }

        savedOrder.setOrderDetails(orderDetails);

        Order finalOrder = orderRepository.save(order);

        updateWarehouseStockForOrder(finalOrder);

        log.info("Order created successfully -Number: {}, Amount: {}",
                finalOrder.getOrderNumber(), finalOrder.getAmount());

        return orderMapper.toResponse(finalOrder);
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
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest updateOrderStatusRequest) {
        log.info("Updating order status: {} to {}", id, updateOrderStatusRequest.getOrderStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));


        if (updateOrderStatusRequest.getNotes() != null &&
                updateOrderStatusRequest.getNotes().trim().isEmpty()) {
            order.setNotes(updateOrderStatusRequest.getNotes());
        }

        if (updateOrderStatusRequest.getOrderStatus() == OrderStatus.COMPLETED &&
                order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());

            log.info("Payment status automatically updated to PAID for completed order");
        }

        Order updatedOrder = orderRepository.save(order);

        return orderMapper.toResponse(updatedOrder);
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

        if (driver.getDriverStatus() != DriverStatus.ACTIVE) {
            throw new RuntimeException("Driver status is not ACTIVE");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order status is not PENDING");
        }

        if (order.getDriver() != null){
            throw new RuntimeException("Driver already assigned");
        }

        approveOrder(order.getId());
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

        int emptyBottlesCollected = order.getEmptyBottlesCollected();

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setEmptyBottlesCollected(emptyBottlesCollected);
        order.setCompletedAt(LocalDateTime.now());

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
}
