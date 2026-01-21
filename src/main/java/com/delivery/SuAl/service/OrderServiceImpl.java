package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.entity.UserContainer;
import com.delivery.SuAl.exception.BusinessRuleViolationException;
import com.delivery.SuAl.exception.InvalidOrderStateException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.PaymentRefundException;
import com.delivery.SuAl.exception.UnauthorizedOperationException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.EligibleCampaignInfo;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.enums.OperatorStatus;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.request.cart.CalculatePriceRequest;
import com.delivery.SuAl.model.request.cart.CartItem;
import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ApplyPromoRequest;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByOperatorRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByUserRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderItemRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.cart.CartCalculationResponse;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.ApplyPromoResponse;
import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import com.delivery.SuAl.model.response.order.BottleCollectionExpectation;
import com.delivery.SuAl.model.response.order.DriverCollectionInfoResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.order.ProductDeliverItem;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.AddressRepository;
import com.delivery.SuAl.repository.CampaignRepository;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OperatorRepository;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.UserContainerRepository;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;

    private final OrderCalculationService orderCalculationService;
    private final CartPriceCalculationService cartPriceCalculationService;
    private final PromoService promoService;
    private final CampaignService campaignService;
    private final InventoryService inventoryService;
    private final ContainerManagementService containerManagementService;
    private final PaymentService paymentService;

    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderDetailFactory orderDetailFactory;
    private final OrderMapper orderMapper;
    private final UserContainerRepository userContainerRepository;

    @Override
    @Transactional
    public OrderResponse createOrderByUser(String phoneNumber, CreateOrderByUserRequest request) {
        log.info("User creating order - phoneNumber: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found with phoneNumber: " + phoneNumber));

        log.info("User found - userId: {}, name: {}", user.getId(), user.getFirstName());
        return createOrderInternal(user, null, request.getAddressId(), request.getDeliveryDate(),
                request.getItems(), request.getPromoCode(), request.getNote());
    }

    @Override
    @Transactional
    public OrderResponse createOrderByOperator(String operatorEmail, CreateOrderByOperatorRequest request) {
        log.info("Operator creating order - operatorEmail: {}, userId: {}", operatorEmail, request.getUserId());

        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator not found with email: " + operatorEmail));

        User user = findUserById(request.getUserId());
        return createOrderInternal(user, operator, request.getAddressId(), request.getDeliveryDate(),
                request.getItems(), request.getPromoCode(), request.getNotes());
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest updateRequest) {
        log.info("Updating order for Customer {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Order must be PENDING to be updated");
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
            throw new InvalidOrderStateException("Order must be APPROVED before assigning driver");
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
                .orElseThrow(() -> new NotFoundException("Operator " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new UnauthorizedOperationException("Operator is not active with email: " + operatorEmail);
        }
        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Order must be PENDING before approving order");
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
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            product.setOrderCount(product.getOrderCount() + detail.getCount());
            productRepository.save(product);
        }

        order.setOrderStatus(OrderStatus.APPROVED);
        order.setOperator(operator);
        Order savedOrder = orderRepository.save(order);

        log.info("Order approved successfully: {}", orderId);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse rejectOrderByUser(String phoneNumber, Long orderId, String reason) {
        log.info("User {} rejecting order ID: {}", phoneNumber, orderId);

        Order order = findOrderById(orderId);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User " + phoneNumber + " not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("User " + user.getId() + " cannot reject order " + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("User can only reject PENDING orders");
        }

        refundPayment(order);

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        Order savedOrder = orderRepository.save(order);

        log.info("Order {} rejected by user {}", orderId, user.getId());
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse rejectOrderByOperator(String operatorEmail, Long orderId, String reason) {
        log.info("Operator {} rejecting order ID: {} with reason: {}", operatorEmail, orderId, reason);

        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new UnauthorizedOperationException("Operator is not active with email: " + operatorEmail);
        }

        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException("Order must be PENDING or APPROVED to reject");
        }

        refundPayment(order);

        if (order.getOrderStatus() == OrderStatus.APPROVED) {
            log.info("Order was APPROVED, releasing warehouse stock");

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

            for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                inventoryService.releaseStock(entry.getKey(), entry.getValue());
                log.debug("Released {} units of product {} back to warehouse",
                        entry.getValue(), entry.getKey());
            }

            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                product.setOrderCount(Math.max(0, product.getOrderCount() - detail.getCount()));
                productRepository.save(product);
            }
        }

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        order.setOperator(operator);
        Order savedOrder = orderRepository.save(order);

        log.info("Order {} rejected by operator {}", orderId, operatorEmail);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest) {
        log.info("Completing order {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException("Order must be APPROVED before completing order");
        }

        if (order.getDriver() == null)
            throw new InvalidOrderStateException("Driver must be assigned before completing order");

        validateCollectedBottles(order, completeDeliveryRequest.getBottlesCollected());

        log.info("Adding delivered products to user container balance");
        containerManagementService.processDeliveredProducts(
                order.getUser().getId(),
                order.getOrderDetails()
        );

        log.info("Processing collected bottles");
        containerManagementService.processCollectedBottles(
                order.getUser().getId(),
                order.getOrderDetails(),
                completeDeliveryRequest.getBottlesCollected()
        );

        int totalExpected = order.getEmptyBottlesExpected();
        int totalCollected = calculateTotalBottlesCollected(order);
        int missingBottles = totalExpected - totalCollected;

        log.info("Container collection -Expected: {}, Collected: {}, Missing: {}",
                totalExpected, totalCollected, missingBottles);

        orderCalculationService.recalculateDepositsFromActualCollection(order);

        if (missingBottles > 0) {
            BigDecimal depositPerUnit = order.getOrderDetails().isEmpty()
                    ? BigDecimal.ZERO
                    : order.getOrderDetails().getFirst().getDepositPerUnit();

            BigDecimal extraDepositCharge = depositPerUnit
                    .multiply(BigDecimal.valueOf(missingBottles))
                    .setScale(2, RoundingMode.HALF_UP);

            log.warn("USER DID NOT RETURN {} CONTAINERS - Extra deposit kept: {} ({}×{})",
                    missingBottles, extraDepositCharge, missingBottles, depositPerUnit);

            String missingContainerNote = String.format(
                    "Missing containers: %d bottles not returned. Deposit not refunded: %s AZN (%s × %d)",
                    missingBottles, extraDepositCharge, depositPerUnit, missingBottles
            );
            appendNotes(order, missingContainerNote);

            log.info("Deposit kept for missing containers: {}", extraDepositCharge);
        }

        order.setEmptyBottlesCollected(totalCollected);
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        if (order.getPaymentMethod() == PaymentMethod.CASH &&
                order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setPaidAt(LocalDateTime.now());
        } else if (order.getPaymentMethod() == PaymentMethod.CARD) {
            if (missingBottles > 0){
                BigDecimal originalAmount = order.getSubtotal().add(order.getTotalDepositCharged())
                        .subtract(order.getTotalDepositRefunded());
                BigDecimal difference = order.getTotalAmount().subtract(originalAmount);

                if (difference.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("Order was paid online but deposit of {} was not refunded. " +
                            "Amount difference: {}", difference, difference);

                    appendNotes(order,
                            "Note: Online payment - Deposit " + difference +
                            " AZN kept for " + missingBottles + " missing containers");
                }
            }
        }

        appendNotes(order, completeDeliveryRequest.getNotes());

        Order savedOrder = orderRepository.save(order);

        log.info("Order {} completed successfully", savedOrder.getOrderNumber());
        log.info("Expected bottles: {}, Collected: {}, Missing: {}",
                totalExpected, totalCollected, missingBottles);
        log.info("Delivered: {}, Final amount: {}",
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
    public PageResponse<OrderResponse> getAllOrdersByUser(Pageable pageable, String phoneNumber) {
        log.info("Fetching all orders for user phoneNumber {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found with phoneNumber: " + phoneNumber));

        Page<Order> orderPage = orderRepository.findByUserId(user.getId(), pageable);

        List<Order> orders = orderPage.getContent();

        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();
        return PageResponse.of(responses, orderPage);
    }

    @Override
    public DriverCollectionInfoResponse getDriverCollectionInfo(Long orderId) {
        log.info("Getting driver collection info for order {}", orderId);

        Order order = findOrderById(orderId);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(
                    "Order must be APPROVED to get driver collection info. Current state: "
                            + order.getOrderStatus());
        }

        Map<Long, Integer> userContainerBalances = new HashMap<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            Optional<UserContainer> containerOpt = userContainerRepository
                    .findByUserIdAndProductId(order.getUser().getId(), detail.getProduct().getId());

            int available = containerOpt.map(UserContainer::getQuantity).orElse(0);
            userContainerBalances.put(detail.getProduct().getId(), available);
        }

        List<ProductDeliverItem> deliverItems = new ArrayList<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            deliverItems.add(ProductDeliverItem.builder()
                    .productId(detail.getProduct().getId())
                    .productName(detail.getProduct().getName())
                    .quantity(detail.getCount())
                    .pricePerUnit(detail.getPricePerUnit())
                    .depositPerUnit(detail.getDepositPerUnit())
                    .build());
        }

        List<BottleCollectionExpectation> expectations = new ArrayList<>();
        boolean hasInsufficientContainers = false;
        BigDecimal totalPotentialExtraDeposit = BigDecimal.ZERO;
        for (OrderDetail detail : order.getOrderDetails()) {
            int expectedToCollect = detail.getContainersReturned();
            int userHas = userContainerBalances.getOrDefault(detail.getProduct().getId(), 0);
            int shortfall = Math.max(0, expectedToCollect - userHas);

            BigDecimal extraDepositPerBottle = detail.getDepositPerUnit();
            BigDecimal totalExtraDeposit = extraDepositPerBottle
                    .multiply(BigDecimal.valueOf(shortfall))
                    .setScale(2, RoundingMode.HALF_UP);

            String warningMessage = null;
            if (shortfall > 0) {
                hasInsufficientContainers = true;
                totalPotentialExtraDeposit = totalPotentialExtraDeposit.add(totalExtraDeposit);
                warningMessage = String.format(
                        "User only has %d/%d containers. Collect extra %s AZN if missing!",
                        userHas, expectedToCollect, totalExtraDeposit
                );
            }

            expectations.add(BottleCollectionExpectation.builder()
                    .productId(detail.getProduct().getId())
                    .productName(detail.getProduct().getName())
                    .expectedToCollect(expectedToCollect)
                    .userHasAvailable(userHas)
                    .shortfall(shortfall)
                    .extraDepositPerBottle(extraDepositPerBottle)
                    .totalExtraDeposit(totalExtraDeposit)
                    .warningMessage(warningMessage)
                    .build());
        }

        String collectionWarning = null;
        if (hasInsufficientContainers) {
            collectionWarning = String.format(
                    "ATTENTION: User doesn't have all expected containers! \" +\n" +
                            "            \"If user cannot return missing containers, collect EXTRA %s AZN deposit. \" +\n" +
                            "            \"Original amount: %s → Possible amount: %s",
                    totalPotentialExtraDeposit,
                    order.getTotalAmount(),
                    order.getTotalAmount().add(totalPotentialExtraDeposit)
            );
        }
        return DriverCollectionInfoResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .userPhone(order.getUser().getPhoneNumber())
                .deliveryAddress(order.getAddress().getFullAddress())
                .productsToDeliver(deliverItems)
                .totalBottlesExpected(order.getEmptyBottlesExpected())
                .expectedCollections(expectations)
                .estimatedTotalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .hasInsufficientContainers(hasInsufficientContainers)
                .collectionWarning(collectionWarning)
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id " + userId));
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order Not Found with id: " + id));
    }

    private Address findUserAddress(Long userId, Long addressId) {
        return addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new NotFoundException("Address not found with id: " + addressId + " for user: " + userId));
    }

    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver Not Found with id " + id));
    }

    private OrderResponse createOrderInternal(
            User user,
            Operator operator,
            Long addressId,
            LocalDate deliveryDate,
            List<CartItem> items,
            String promoCode,
            String notes) {

        log.info("Creating order for user ID: {}, operator: {}, items: {}",
                user.getId(),
                operator != null ? operator.getId() : "none",
                items.size());

        Address address = findUserAddress(user.getId(), addressId);

        CalculatePriceRequest priceRequest = new CalculatePriceRequest();
        priceRequest.setUserId(user.getId());
        priceRequest.setItems(items);
        priceRequest.setPromoCode(promoCode);

        CartCalculationResponse pricing = cartPriceCalculationService.calculatePrice(priceRequest);

        log.debug("Cart calculation: subtotal={}, promo={}, netDeposit={}, total={}",
                pricing.getSubtotal(),
                pricing.getPromoDiscount(),
                pricing.getNetDeposit(),
                pricing.getTotalAmount());

        Map<Long, Integer> productQuantities = items.stream()
                .collect(Collectors.toMap(
                        CartItem::getProductId,
                        CartItem::getQuantity
                ));

        ContainerDepositSummary depositSummary =
                containerManagementService.calculateAvailableContainerRefunds(
                        user.getId(),
                        productQuantities
                );

        String orderNumber = orderNumberGenerator.generateOrderNumber();
        log.info("Generated order number: {}", orderNumber);

        Order order = new Order();
        order.setUser(user);
        order.setOperator(operator);
        order.setAddress(address);
        order.setOrderNumber(orderNumber);
        order.setEmptyBottlesExpected(depositSummary.getTotalContainersUsed());
        order.setEmptyBottlesCollected(0);
        order.setNotes(notes);
        order.setDeliveryDate(deliveryDate);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.CASH);

        order.setSubtotal(pricing.getSubtotal());
        order.setTotalDepositCharged(pricing.getTotalDepositCharged());
        order.setTotalDepositRefunded(pricing.getTotalDepositRefunded());
        order.setNetDeposit(pricing.getNetDeposit());
        order.setTotalItems(pricing.getTotalItems());
        order.setPromoDiscount(pricing.getPromoDiscount() != null
                ? pricing.getPromoDiscount()
                : BigDecimal.ZERO);

        boolean willUserPromo = promoCode != null && !promoCode.trim().isEmpty();
        BigDecimal campaignBonusValue = BigDecimal.ZERO;
        List<EligibleCampaignInfo> campaignToApply = List.of();

        if (!willUserPromo) {
            GetEligibleCampaignsRequest campaignsRequest = GetEligibleCampaignsRequest.builder()
                    .userId(user.getId())
                    .productQuantities(productQuantities)
                    .willUsePromoCode(willUserPromo)
                    .build();

            EligibleCampaignsResponse eligibleCampaigns =
                    campaignService.getEligibleCampaigns(campaignsRequest);

            campaignToApply = eligibleCampaigns
                    .getEligibleCampaigns()
                    .stream()
                    .filter(c -> Boolean.TRUE.equals(c.getWillBeApplied()))
                    .toList();

            campaignBonusValue = eligibleCampaigns.getTotalCampaignDiscount();
        }

        order.setCampaignDiscount(campaignBonusValue);

        BigDecimal amount = order.getSubtotal()
                .subtract(order.getPromoDiscount());
        BigDecimal totalAmount = amount.add(order.getNetDeposit());

        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        List<OrderDetail> orderDetails = orderDetailFactory.createOrderDetailsFromCart(
                items,
                depositSummary.getContainersReturnedByProduct()
        );

        Order savedOrder = orderRepository.save(order);

        orderDetails.forEach(detail -> detail.setOrder(savedOrder));
        savedOrder.setOrderDetails(orderDetails);

        log.info("Order saved with ID: {}, number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        if (promoCode != null
                && !promoCode.trim().isEmpty()
                && Boolean.TRUE.equals(pricing.getPromoValid())) {
            try {
                ApplyPromoResponse promoResult = promoService.applyPromo(
                        ApplyPromoRequest.builder()
                                .userId(user.getId())
                                .orderId(savedOrder.getId())
                                .promoCode(promoCode)
                                .orderAmount(pricing.getSubtotal())
                                .build()
                );

                if (Boolean.TRUE.equals(promoResult.getSuccess())) {
                    savedOrder.setPromo(promoService.getPromoEntityByCode(promoCode));
                    log.info("Promo applied: {}, Discount: {}",
                            promoCode,
                            pricing.getPromoDiscount());
                }
            } catch (Exception e) {
                log.error("Failed to apply promo: {}", promoCode, e);
            }
        }

        for (EligibleCampaignInfo campaignInfo : campaignToApply) {
            try {
                ApplyCampaignRequest applyCampaignRequest = ApplyCampaignRequest.builder()
                        .campaignCode(campaignInfo.getCampaignCode())
                        .userId(user.getId())
                        .order(savedOrder)
                        .build();

                ApplyCampaignResponse campaignResult =
                        campaignService.applyCampaign(applyCampaignRequest);

                if (Boolean.TRUE.equals(campaignResult.getSuccess())) {
                    addCampaignFreeProduct(savedOrder, campaignResult);

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

        Order finalOrder = orderRepository.save(savedOrder);

        log.info("Order created successfully: {} - Subtotal: {}, Promo: {}, Campaign: {}, Deposits: {}, Total: {}",
                finalOrder.getOrderNumber(),
                finalOrder.getSubtotal(),
                finalOrder.getPromoDiscount(),
                campaignBonusValue,
                finalOrder.getNetDeposit(),
                finalOrder.getTotalAmount());

        return orderMapper.toResponse(finalOrder);
    }

    private void addCampaignFreeProduct(Order order, ApplyCampaignResponse campaignResult) {
        Product freeProduct = productRepository.findById(campaignResult.getFreeProductId())
                .orElseThrow(() -> new NotFoundException(
                        "Free product not found: " + campaignResult.getFreeProductId()));

        Campaign campaign = campaignRepository.findByCampaignCode(campaignResult.getCampaignCode())
                .orElseThrow(() -> new NotFoundException(
                        "Campaign not found: " + campaignResult.getCampaignCode()));

        OrderCampaignBonus campaignBonus = new OrderCampaignBonus();
        campaignBonus.setOrder(order);
        campaignBonus.setCampaign(campaign);
        campaignBonus.setProduct(freeProduct);
        campaignBonus.setQuantity(campaignResult.getFreeQuantity());
        campaignBonus.setOriginalValue(campaignResult.getBonusValue());

        order.getCampaignBonuses().add(campaignBonus);

        log.info("Added campaign bonus: {} x {} - value: {}",
                freeProduct.getName(),
                campaignResult.getFreeQuantity(),
                campaignResult.getBonusValue());
    }

    private void applyContainerInfoToOrderDetails(
            List<OrderDetail> orderDetails,
            ContainerDepositSummary depositSummary
    ) {
        Map<Long, ProductDepositInfo> depositInfoMap = depositSummary
                .getProductDepositInfos().stream()
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
            throw new BusinessRuleViolationException("Bottle collection validation failed: " + String.join(", ", error));
        }
    }

    private int calculateTotalBottlesCollected(Order order) {
        return order.getOrderDetails().stream()
                .mapToInt(OrderDetail::getContainersReturned)
                .sum();
    }

    private void refundPayment(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            try {
                log.info("Order {} has successful payment, initiating refund", order.getId());
                paymentService.refundPayment(order.getId());
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            } catch (Exception e) {
                log.error("Failed to refund payment for order {}", order.getId(), e);
                throw new PaymentRefundException("Could not process refund: " + e.getMessage(), e);
            }
        }
    }
}