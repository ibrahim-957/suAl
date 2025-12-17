package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentStatus;
import com.delivery.SuAl.model.request.operation.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.repository.AddressRepository;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final DriverRepository driverRepository;
    private final OrderCalculationService orderCalculationService;
    private final PromoCodeService promoCodeService;
    private final InventoryService  inventoryService;
    private final OrderNumberGenerator  orderNumberGenerator;
    private final OrderDetailFactory  orderDetailFactory;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        log.info("Creating new order for Customer {}", createOrderRequest.getCustomerName());

        Address address = findAddress(createOrderRequest.getAddressId());

        List<OrderDetail>  orderDetails = createOrderDetails(createOrderRequest.getItems());

        Order order = initializeOrder(createOrderRequest, address, orderDetails);

        OrderCalculationResult calculation = orderCalculationService.calculateOrderTotals(
                orderDetails, createOrderRequest.getEmptyBottlesExpected()
        );

        PromoDiscountResult promoDiscountResult = promoCodeService.applyPromoCode(
                createOrderRequest.getPromoCode(), calculation.getSubtotal()
        );

        applyCalculationToOrder(order, calculation, promoDiscountResult);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest updateRequest) {
        log.info("Updating order for Customer {}", orderId);

        Order order = findOrderById(orderId);
        boolean needsRecalculation  = false;

        if (updateRequest.getNotes() != null){
            order.setNotes(updateRequest.getNotes());
        }

        if (updateRequest.getDeliveryDate() != null) {
            order.setDeliveryDate(updateRequest.getDeliveryDate());
        }

        if (updateRequest.getAddressId() != null) {
            order.setAddress(findAddress(updateRequest.getAddressId()));
        }

        if (updateRequest.getItems() != null && !updateRequest.getItems().isEmpty()) {
            updateOrderItems(order, updateRequest.getItems());
            needsRecalculation = true;
        }

        if (needsRecalculation || updateRequest.getEmptyBottlesExpected() != null) {
            recalculateOrder(order, updateRequest.getEmptyBottlesExpected());
        }

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Getting order for Customer {}", id);
        Order order = findOrderById(id);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = findOrderById(id);

        order.getOrderDetails().forEach(detail ->
                inventoryService.releaseStock(detail.getProduct().getId(), detail.getCount())
        );

        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public OrderResponse assignDriver(Long orderId, Long driverId) {
        Order order = findOrderById(orderId);
        Driver driver = findDriverById(driverId);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Order must be approved before assigning driver");
        }
        order.setDriver(driver);
        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(Long orderId) {
        Order order = findOrderById(orderId);

        if(order.getOrderStatus() != OrderStatus.PENDING){
            throw new RuntimeException("Order must be pending before approving order");
        }
        order.setOrderStatus(OrderStatus.APPROVED);
        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(Long orderId, String reason) {
        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order must be pending before rejecting order");
        }

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest) {
        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Order must be approved before completing order");
        }

        if (order.getDriver() == null) {
            throw new RuntimeException("Order must have a driver assigned");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setCompletedAt(LocalDateTime.now());

        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }

        int emptyBottlesCollected = order.getEmptyBottlesCollected();
        order.setEmptyBottlesCollected(emptyBottlesCollected);

        if (emptyBottlesCollected > 0) {
            OrderDetail firstDetail = order.getOrderDetails().getFirst();
            inventoryService.addEmptyBottles(
                    firstDetail.getProduct().getId(),
                    emptyBottlesCollected
            );
        }

        if (completeDeliveryRequest.getNotes() != null && !completeDeliveryRequest.getNotes().trim().isEmpty()) {
            appendNotes(order, completeDeliveryRequest.getNotes());
        }

        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTodaysOrders() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return orderRepository.countTodaysOrders(startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.calculateRevenue(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTodaysRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return calculateRevenue(startOfDay, endOfDay);
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
    }

    private Address findAddress(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address Not Found"));
    }

    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver Not Found"));
    }

    private List<OrderDetail> createOrderDetails(List<OrderItemRequest> orderItemRequests) {
       List<OrderDetail> orderDetails = new ArrayList<>();

       for(OrderItemRequest orderItemRequest : orderItemRequests) {
           inventoryService.validateAndReserveStock(orderItemRequest.getProductId(), orderItemRequest.getQuantity());
           OrderDetail orderDetail = orderDetailFactory.createOrderDetail(orderItemRequest);
           orderDetails.add(orderDetail);
       }
       return orderDetails;
    }

    private Order initializeOrder(CreateOrderRequest createOrderRequest, Address address, List<OrderDetail> orderDetails) {
        Order order = orderMapper.toEntity(createOrderRequest);
        order.setAddress(address);
        order.setOrderNumber(orderNumberGenerator.generateOrderNumber());
        order.setEmptyBottlesCollected(
                createOrderRequest.getEmptyBottlesExpected() != null ?  createOrderRequest.getEmptyBottlesExpected() : 0
        );
        order.setOrderDetails(orderDetails);
        orderDetails.forEach(orderDetail -> orderDetail.setOrder(order));

        return order;
    }

    private void applyCalculationToOrder(Order order, OrderCalculationResult calculation, PromoDiscountResult promoResult){
        order.setCount(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(calculation.getTotalDepositRefunded());
        order.setNetDeposit(calculation.getNetDeposit());

        if (promoResult.hasPromo()) {
            order.setPromo(promoResult.getPromo());
            order.setPromoDiscount(promoResult.getDiscount());
        }

        BigDecimal totalAmount = calculation.getSubtotal()
                .subtract(promoResult.getDiscount());
        BigDecimal finalAmount = totalAmount.add(calculation.getNetDeposit());

        order.setTotalAmount(totalAmount);
        order.setAmount(finalAmount);
    }

    private void updateOrderItems(Order order, List<UpdateOrderItemRequest> itemUpdates) {
        Map<Long, OrderDetail> detailMap = order.getOrderDetails().stream()
                .collect(Collectors.toMap(OrderDetail::getId, d -> d));

        for (UpdateOrderItemRequest update : itemUpdates) {
            OrderDetail detail = detailMap.get(update.getOrderDetailId());
            if (detail == null) continue;

            if (update.getQuantity() != null) {
                int quantityDiff = update.getQuantity() - detail.getCount();
                inventoryService.adjustStock(detail.getProduct().getId(), -quantityDiff);
                detail.setCount(update.getQuantity());
            }

            if (update.getSellPrice() != null) {
                detail.setPricePerUnit(update.getSellPrice());
            }

            orderCalculationService.recalculateOrderDetail(detail);
        }
    }

    private void recalculateOrder(Order order, Integer newEmptyBottlesExpected) {
        if (newEmptyBottlesExpected != null) {
            order.setEmptyBottlesExpected(newEmptyBottlesExpected);
        }

        OrderCalculationResult calculation = orderCalculationService.calculateOrderTotals(
                order.getOrderDetails(), order.getEmptyBottlesExpected()
        );

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        order.setCount(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(calculation.getTotalDepositRefunded());
        order.setNetDeposit(calculation.getNetDeposit());

        BigDecimal totalAmount = calculation.getSubtotal().subtract(promoDiscount);
        BigDecimal finalAmount = totalAmount.add(calculation.getNetDeposit());

        order.setTotalAmount(totalAmount);
        order.setAmount(finalAmount);
    }

    private void appendNotes(Order order, String newNotes) {
        String existingNotes = order.getNotes();
        if (existingNotes != null && !existingNotes.trim().isEmpty()) {
            order.setNotes(existingNotes + " /// " + newNotes);
        } else {
            order.setNotes(newNotes);
        }
    }
}


