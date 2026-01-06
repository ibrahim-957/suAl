package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.InvalidRequestException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.EligibleCampaignInfo;
import com.delivery.SuAl.helper.OrderCalculationResult;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.OperatorStatus;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentMethod;
import com.delivery.SuAl.model.PaymentStatus;
import com.delivery.SuAl.model.request.basket.CreateOrderFromBasketByOperatorRequest;
import com.delivery.SuAl.model.request.basket.CreateOrderFromBasketRequest;
import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ApplyPromoRequest;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.basket.BasketResponse;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.ApplyPromoResponse;
import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.AddressRepository;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OperatorRepository;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final ProductRepository productRepository;

    private final OrderCalculationService orderCalculationService;
    private final PromoService promoService;
    private final CampaignService campaignService;
    private final InventoryService inventoryService;
    private final ContainerManagementService containerManagementService;
    private final BasketService basketService;

    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderDetailFactory orderDetailFactory;
    private final OrderMapper orderMapper;


    @Override
    @Transactional
    public OrderResponse createOrderFromBasketByUser(String phoneNumber, CreateOrderFromBasketRequest request) {
        log.info("User with phone {} creating order from basket", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User with phone number " + phoneNumber + " not found"));

        if (!user.getIsActive())
            throw new InvalidRequestException("User account is not active");

        BasketResponse basket;
        try {
            basket = basketService.getBasket(user.getId());
        } catch (NotFoundException e) {
            log.error("Basket not found for user {}", user.getId());
            throw new InvalidRequestException("Basket not found. Please add items to basket first.");
        }

        if (basket.getTotalItems() == null || basket.getTotalItems() == 0) {
            log.error("Empty basket for user {}", user.getId());
            throw new InvalidRequestException("Cannot create order from empty basket");
        }

        log.info("Creating order from basket for user {} with {} items", user.getId(), basket.getTotalItems());

        CreateOrderRequest createOrderRequest = basketService.convertBasketToOrderRequest(user.getId(), request);

        OrderResponse orderResponse = createOrderInternal(createOrderRequest, user, null);

        clearBasketAfterOrder(user.getId());
        return orderResponse;
    }

    @Override
    @Transactional
    public OrderResponse createOrderFromBasketByOperator(String operatorEmail, CreateOrderFromBasketByOperatorRequest request) {
        log.info("Operator {} creating order from basket for user ID: {}", operatorEmail, request.getUserId());

        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator with email " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new InvalidRequestException("Operator status is not active");
        }

        if (request.getUserId() == null) {
            throw new InvalidRequestException("User ID is required when operator creates order from basket");
        }

        User user = findUserById(request.getUserId());

        BasketResponse basket;
        try {
            basket = basketService.getBasket(request.getUserId());
        } catch (NotFoundException e) {
            log.error("Basket not found for user {}", request.getUserId());
            throw new InvalidRequestException("Basket not found for user " + request.getUserId());
        }

        if (basket.getTotalItems() == null || basket.getTotalItems() == 0) {
            log.error("Empty basket for user {}", request.getUserId());
            throw new InvalidRequestException("Cannot create order from empty basket for user");
        }

        log.info("Operator {} creating order from basket for user {} with {} items",
                operatorEmail, request.getUserId(), basket.getTotalItems());

        CreateOrderFromBasketRequest basketRequest = CreateOrderFromBasketRequest.builder()
                .addressId(request.getAddressId())
                .deliveryDate(request.getDeliveryDate())
                .promoCode(request.getPromoCode())
                .notes(request.getNotes())
                .build();

        CreateOrderRequest createOrderRequest = basketService.convertBasketToOrderRequest(user.getId(), basketRequest);

        OrderResponse orderResponse = createOrderInternal(createOrderRequest, user, operator);

        clearBasketAfterOrder(user.getId());
        return orderResponse;
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest updateRequest) {
        log.info("Updating order for Customer {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Can only update order with PENDING status");
        }

        boolean needsRecalculation = false;

        if (updateRequest.getNotes() != null) {
            order.setNotes(updateRequest.getNotes());
        }

        if (updateRequest.getDeliveryDate() != null) {
            order.setDeliveryDate(updateRequest.getDeliveryDate());
        }

        if (updateRequest.getAddressId() != null) {
            Address newAddress = findUserAddress(order.getUser().getId(), updateRequest.getAddressId());
            order.setAddress(newAddress);
        }

        if (updateRequest.getItems() != null && !updateRequest.getItems().isEmpty()) {
            updateOrderItems(order, updateRequest.getItems());
            needsRecalculation = true;
        }

        if (needsRecalculation) {
            recalculateOrder(order);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order updated successfully: {}", orderId);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Getting order {}", id);
        Order order = findOrderById(id);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse assignDriver(Long orderId, Long driverId) {
        log.info("Assigning driver {} to order {}", driverId, orderId);

        Order order = findOrderById(orderId);
        Driver driver = findDriverById(driverId);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Order must be approved before assigning driver");
        }
        order.setDriver(driver);
        orderRepository.save(order);

        log.info("Driver assigned successfully to order {}", orderId);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(String operatorEmail, Long orderId) {
        log.info("Approving order ID: {}", orderId);
        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new RuntimeException("Operator " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new RuntimeException("Operator is not active with email: " + operatorEmail);
        }
        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order must be pending before approving order");
        }

        Map<Long, Integer> productQuantities = order.getOrderDetails().stream()
                .collect(Collectors.toMap(
                        detail -> detail.getProduct().getId(),
                        OrderDetail::getCount,
                        Integer::sum
                ));

        if (!order.getCampaignBonuses().isEmpty()) {
            for (OrderCampaignBonus bonus : order.getCampaignBonuses()) {
                productQuantities.merge(
                        bonus.getProduct().getId(),
                        bonus.getQuantity(),
                        Integer::sum
                );
            }
        }

        inventoryService.validateAndReserveStockBatch(productQuantities);

        order.setOrderStatus(OrderStatus.APPROVED);
        order.setOperator(operator);
        Order savedOrder = orderRepository.save(order);

        log.info("Order approved successfully: {}", orderId);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(String operatorEmail, Long orderId, String reason) {
        log.info("Rejecting order ID: {} with reason: {}", orderId, reason);
        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new RuntimeException("Operator " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new RuntimeException("Operator is not active with email: " + operatorEmail);
        }
        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order must be pending before rejecting order");
        }

        containerManagementService.releaseReservedContainers(order);

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        order.setOperator(operator);
        Order savedOrder = orderRepository.save(order);

        log.info("Order for Customer {} has been rejected", orderId);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest) {
        log.info("Completing order {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Order must be approved before completing order");
        }

        if (order.getDriver() == null)
            throw new RuntimeException("Driver must be assigned before completing order");

        validateCollectedBottles(order, completeDeliveryRequest.getBottlesCollected());

        containerManagementService.processCollectedBottles(
                order.getUser().getId(),
                order.getOrderDetails(),
                completeDeliveryRequest.getBottlesCollected()
        );

        containerManagementService.processDeliveredProducts(
                order.getUser().getId(),
                order.getOrderDetails()
        );

        orderCalculationService.recalculateDepositsFromActualCollection(order);

        int totalCollected = calculateTotalBottlesCollected(order);
        order.setEmptyBottlesCollected(totalCollected);

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        if (order.getPaymentMethod() == PaymentMethod.CASH &&
                order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setPaidAt(LocalDateTime.now());
        }

        appendNotes(order, completeDeliveryRequest.getNotes());

        Order savedOrder = orderRepository.save(order);

        log.info("Order {} completed - Expected bottles: {}, Collected: {}, Delivered: {}, Final amount: {}",
                savedOrder.getOrderNumber(),
                order.getEmptyBottlesExpected(),
                totalCollected,
                order.getOrderDetails().stream().mapToInt(OrderDetail::getCount).sum(),
                savedOrder.getTotalAmount());
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTodayOrders() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return orderRepository.countTodayOrders(startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.calculateRevenue(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getPendingOrders(Pageable pageable) {
        log.info("Getting pending orders with page: {}", pageable);
        Page<Order> orderPage = orderRepository.findByOrderStatus(OrderStatus.PENDING, pageable);

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrdersForManagement(Pageable pageable) {
        log.info("Getting all orders for management with page: {}", pageable);

        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<Order> orders = orderPage.getContent();

        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return PageResponse.of(responses, orderPage);
    }

    @Override
    public int getCompletedOrderCount(Long userId) {
        log.info("Getting completed orders for user {}", userId);
        return orderRepository.countByUserIdAndOrderStatus(userId, OrderStatus.COMPLETED);
    }

    @Override
    public Order getOrderEntityById(Long orderId) {
        log.info("Fetching order entity by id: {}", orderId);

        return orderRepository.findByUserId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

    }

    private User findUserById(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found with id: " + id));
    }

    private Address findUserAddress(Long userId, Long addressId) {
        return addressRepository
                .findByIdAndUserIdAndIsActiveTrue(userId, addressId)
                .orElseThrow(() -> new RuntimeException("Address Not Found with id: " + addressId + " for user: " + userId));
    }

    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver Not Found with id " + id));
    }

    private OrderResponse createOrderInternal(CreateOrderRequest createOrderRequest, User user, Operator operator) {
        log.info("Creating order for User ID: {}, Operator: {}", user.getId(),
                operator != null ? operator.getEmail() : "none");

        Address address = findUserAddress(createOrderRequest.getAddressId(), user.getId());

        List<OrderDetail> orderDetails = orderDetailFactory.createOrderDetails(createOrderRequest.getItems());

        Map<Long, Integer> productQuantities = createOrderRequest.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::getProductId,
                        OrderItemRequest::getQuantity
                ));

        ContainerDepositSummary depositSummary = containerManagementService.calculateAvailableContainerRefunds(
                user.getId(), productQuantities);

        applyContainerInfoToOrderDetails(orderDetails, depositSummary);

        Order order = initializeOrder(createOrderRequest, user, address, orderDetails, depositSummary, operator);

        OrderCalculationResult calculation = orderCalculationService.calculateOrderTotals(orderDetails);

        order.setTotalItems(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(calculation.getTotalDepositRefunded());
        order.setNetDeposit(calculation.getNetDeposit());

        order.setCampaignDiscount(BigDecimal.ZERO);
        order.setPromoDiscount(BigDecimal.ZERO);

        BigDecimal amount = order.getSubtotal();
        BigDecimal totalAmount = amount.add(order.getNetDeposit());
        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        BigDecimal promoDiscount = BigDecimal.ZERO;
        if (createOrderRequest.getPromoCode() != null && !createOrderRequest.getPromoCode().trim().isEmpty()) {
            Order tempSavedOrder = orderRepository.save(order);

            ApplyPromoResponse promoResult = promoService.applyPromo(
                    ApplyPromoRequest.builder()
                            .userId(user.getId())
                            .orderId(tempSavedOrder.getId())
                            .promoCode(createOrderRequest.getPromoCode())
                            .orderAmount(calculation.getSubtotal())
                            .build()
            );

            if (promoResult.getSuccess()) {
                order.setPromoDiscount(promoResult.getDiscountApplied());
                order.setPromo(promoService.getPromoEntityByCode(createOrderRequest.getPromoCode()));
                promoDiscount = promoResult.getDiscountApplied();
                log.info("Promo applied: {}, Discount: {}", createOrderRequest.getPromoCode(), promoDiscount);
            }
        }

        BigDecimal campaignBonusValue = BigDecimal.ZERO;

        boolean willUsePromo = (createOrderRequest.getPromoCode() != null && !createOrderRequest.getPromoCode().trim().isEmpty());

        GetEligibleCampaignsRequest campaignRequest = GetEligibleCampaignsRequest.builder()
                .userId(user.getId())
                .productQuantities(productQuantities)
                .willUsePromoCode(willUsePromo)
                .build();

        EligibleCampaignsResponse eligibleCampaigns = campaignService.getEligibleCampaigns(campaignRequest);

        List<EligibleCampaignInfo> campaignsToApply = eligibleCampaigns.getEligibleCampaigns().stream()
                .filter(c -> Boolean.TRUE.equals(c.getWillBeApplied()))
                .toList();

        if (!campaignsToApply.isEmpty()) {
            if (order.getId() == null) {
                order = orderRepository.save(order);
            }

            for (EligibleCampaignInfo campaignInfo : campaignsToApply) {
                try {
                    ApplyCampaignRequest applyCampaignRequest = ApplyCampaignRequest.builder()
                            .campaignCode(campaignInfo.getCampaignCode())
                            .userId(user.getId())
                            .orderId(order.getId())
                            .build();

                    ApplyCampaignResponse campaignResult = campaignService.applyCampaign(applyCampaignRequest);

                    if (campaignResult.getSuccess()) {
                        Product freeProduct = productRepository.findById(campaignResult.getFreeProductId())
                                .orElseThrow(() -> new NotFoundException("Free product not found: " + campaignResult.getFreeProductId()));

                        OrderDetail freeOrderDetail = new OrderDetail();
                        freeOrderDetail.setOrder(order);
                        freeOrderDetail.setProduct(freeProduct);
                        freeOrderDetail.setCompany(freeProduct.getCompany());
                        freeOrderDetail.setCategory(freeProduct.getCategory());
                        freeOrderDetail.setPricePerUnit(BigDecimal.ZERO);
                        freeOrderDetail.setBuyPrice(freeProduct.getPrices().getLast().getBuyPrice());
                        freeOrderDetail.setCount(campaignResult.getFreeQuantity());
                        freeOrderDetail.setSubtotal(BigDecimal.ZERO);

                        if (Boolean.TRUE.equals(freeProduct.getHasDeposit())) {
                            BigDecimal depositPerUnit = freeProduct.getDepositAmount();
                            freeOrderDetail.setDepositPerUnit(depositPerUnit);
                            freeOrderDetail.setDepositCharged(depositPerUnit.multiply(BigDecimal.valueOf(campaignResult.getFreeQuantity())));
                            freeOrderDetail.setDepositRefunded(BigDecimal.ZERO);
                            freeOrderDetail.setContainersReturned(0);

                            order.setTotalDepositCharged(order.getTotalDepositCharged().add(freeOrderDetail.getDepositCharged()));
                            order.setNetDeposit(order.getNetDeposit().add(freeOrderDetail.getDepositCharged()));
                        } else {
                            freeOrderDetail.setDepositPerUnit(BigDecimal.ZERO);
                            freeOrderDetail.setDepositCharged(BigDecimal.ZERO);
                            freeOrderDetail.setDepositRefunded(BigDecimal.ZERO);
                        }

                        freeOrderDetail.setLineTotal(freeOrderDetail.getSubtotal()
                                .add(freeOrderDetail.getDepositCharged())
                                .subtract(freeOrderDetail.getDepositRefunded()));

                        order.getOrderDetails().add(freeOrderDetail);
                        campaignBonusValue = campaignBonusValue.add(campaignResult.getBonusValue());

                        log.info("Campaign applied: {} - Free product: {} x {}, Bonus value: {}",
                                campaignResult.getCampaignName(),
                                campaignResult.getFreeProductName(),
                                campaignResult.getFreeQuantity(),
                                campaignResult.getBonusValue());
                    }
                } catch (Exception e) {
                    log.error("Error applying campaign: {}", campaignInfo.getCampaignCode(), e);
                }
            }
        }

        order.setCampaignDiscount(campaignBonusValue);

        amount = order.getSubtotal()
                .subtract(promoDiscount)
                .subtract(campaignBonusValue);

        totalAmount = amount.add(order.getNetDeposit());

        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        containerManagementService.reserveContainers(user.getId(), depositSummary);

        Order savedOrder = orderRepository.save(order);

        log.info("Order created: {} - Subtotal: {}, Promo: {}, Campaign: {}, Deposits: {}, Total: {}",
                savedOrder.getOrderNumber(),
                order.getSubtotal(),
                promoDiscount,
                campaignBonusValue,
                order.getNetDeposit(),
                totalAmount);

        return orderMapper.toResponse(savedOrder);
    }

    private Order initializeOrder(
            CreateOrderRequest createOrderRequest,
            User user,
            Address address,
            List<OrderDetail> orderDetails,
            ContainerDepositSummary depositSummary,
            Operator operator) {

        Order order = new Order();
        order.setUser(user);
        order.setOperator(operator);
        order.setAddress(address);
        order.setOrderNumber(orderNumberGenerator.generateOrderNumber());

        order.setEmptyBottlesExpected(depositSummary.getTotalContainersUsed());

        order.setEmptyBottlesCollected(0);
        order.setNotes(createOrderRequest.getNotes());
        order.setDeliveryDate(createOrderRequest.getDeliveryDate());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setOrderDetails(orderDetails);
        orderDetails.forEach(orderDetail -> orderDetail.setOrder(order));

        return order;
    }

    private void applyContainerInfoToOrderDetails(
            List<OrderDetail> orderDetails,
            ContainerDepositSummary depositSummary
    ) {
        Map<Long, ProductDepositInfo> depositInfoMap = depositSummary
                .getProductDepositInfoList().stream()
                .collect(Collectors.toMap(ProductDepositInfo::getProductId, info -> info));

        for (OrderDetail detail : orderDetails) {
            ProductDepositInfo info = depositInfoMap.get(detail.getProduct().getId());
            if (info != null) {
                detail.setContainersReturned(info.getContainersUsed());
                detail.setDepositRefunded(info.getDepositRefund());
                detail.setLineTotal(
                        detail.getSubtotal()
                                .add(detail.getDepositCharged())
                                .subtract(detail.getDepositRefunded())
                );

                log.debug("Applied container info to detail: product={}, container={}, refund={}",
                        detail.getProduct().getId(), info.getContainersUsed(), info.getDepositRefund());
            }
        }
    }

    private void updateOrderItems(Order order, List<UpdateOrderItemRequest> itemUpdates) {
        Map<Long, OrderDetail> detailMap = order.getOrderDetails().stream()
                .collect(Collectors.toMap(OrderDetail::getId, d -> d));

        for (UpdateOrderItemRequest update : itemUpdates) {
            OrderDetail detail = detailMap.get(update.getOrderDetailId());
            if (detail == null) {
                log.warn("Order detail {} not found, skipping update", update.getOrderDetailId());
                continue;
            }

            if (update.getQuantity() != null) {
                int quantityDiff = update.getQuantity() - detail.getCount();
                inventoryService.adjustStock(detail.getProduct().getId(), -quantityDiff);
                detail.setCount(update.getQuantity());

                log.debug("Update quantity for detail {}: {}",
                        detail.getId(), update.getQuantity());
            }

            if (update.getSellPrice() != null) {
                detail.setPricePerUnit(update.getSellPrice());
                log.debug("Update price for detail {}: {}",
                        detail.getId(), update.getSellPrice());
            }

            orderCalculationService.recalculateOrderDetail(detail);
        }
        log.info("Update {} order items", itemUpdates.size());
    }

    private void recalculateOrder(Order order) {
        log.info("Recalculating order {}", order.getOrderNumber());

        Map<Long, Integer> productQuantities = order.getOrderDetails().stream()
                .collect(Collectors.toMap(
                        detail -> detail.getProduct().getId(),
                        OrderDetail::getCount
                ));

        ContainerDepositSummary depositSummary =
                containerManagementService.calculateAvailableContainerRefunds(
                        order.getUser().getId(), productQuantities
                );

        applyContainerInfoToOrderDetails(order.getOrderDetails(), depositSummary);
        orderCalculationService.recalculateOrderFinancials(order);
        order.setEmptyBottlesExpected(depositSummary.getTotalContainersUsed());

        log.info("Order recalculated: subtotal={}, netDeposit={}, total={}",
                order.getSubtotal(), order.getNetDeposit(), order.getTotalAmount());
    }

    private void appendNotes(Order order, String newNotes) {
        if (newNotes == null || newNotes.trim().isEmpty()) {
            return;
        }
        String existingNotes = order.getNotes();
        if (existingNotes != null && !existingNotes.trim().isEmpty()) {
            order.setNotes(existingNotes + " /// " + newNotes);
        } else {
            order.setNotes(newNotes);
        }
        log.info("Notes appended: {}", order.getNotes());
    }

    private void validateCollectedBottles(Order order, List<BottleCollectionItem> bottlesCollected) {
        if (bottlesCollected == null || bottlesCollected.isEmpty()) {
            log.info("No bottles collected for order {}", order.getOrderNumber());
            return;
        }

        Map<Long, OrderDetail> orderDetailMap = order.getOrderDetails().stream()
                .collect(Collectors.toMap(
                        detail -> detail.getProduct().getId(),
                        detail -> detail
                ));

        List<String> error = new ArrayList<>();

        for (BottleCollectionItem item : bottlesCollected) {
            OrderDetail detail = orderDetailMap.get(item.getProductId());

            if (detail == null) {
                error.add(String.format("Product %d was not in this order", item.getProductId()));
                continue;
            }

            if (item.getQuantity() < 0) {
                error.add(String.format("Invalid quantity %d for product %d",
                        item.getQuantity(), item.getProductId()));
                continue;
            }

            if (item.getQuantity() > detail.getCount()) {
                log.warn("Order {}: Collecting {} bottles of product {} but only delivered {}. " +
                                "Customer returned extra bottles from previous orders.",
                        order.getOrderNumber(), item.getQuantity(), item.getProductId(), detail.getCount());
            }
        }

        if (!error.isEmpty()) {
            throw new RuntimeException("Bottle collection validation failed: " + String.join(", ", error));
        }
    }

    private int calculateTotalBottlesCollected(Order order) {
        return order.getOrderDetails().stream()
                .mapToInt(OrderDetail::getContainersReturned)
                .sum();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearBasketAfterOrder(Long userId) {
        try {
            basketService.clearBasket(userId);
            log.info("Basket cleared successfully for user {} after order creation", userId);
        } catch (Exception e) {
            log.error("Failed to clear basket for user {} after order creation: {}", userId, e.getMessage());
        }
    }
}