package com.delivery.SuAl.service;

import com.delivery.SuAl.annotation.SendNotification;
import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.InvalidOrderStateException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.PaymentRefundException;
import com.delivery.SuAl.exception.UnauthorizedOperationException;
import com.delivery.SuAl.helper.CampaignApplicationResult;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.EligibleCampaignInfo;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.enums.NotificationType;
import com.delivery.SuAl.model.enums.OperatorStatus;
import com.delivery.SuAl.model.enums.OperatorType;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.enums.StockReservationType;
import com.delivery.SuAl.model.request.cart.CalculatePriceRequest;
import com.delivery.SuAl.model.request.cart.CartItem;
import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ApplyPromoRequest;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByCustomerRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByOperatorRequest;
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
import com.delivery.SuAl.repository.AdminRepository;
import com.delivery.SuAl.repository.CampaignRepository;
import com.delivery.SuAl.repository.CustomerContainerRepository;
import com.delivery.SuAl.repository.CustomerRepository;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OperatorRepository;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.UserRepository;
import com.delivery.SuAl.security.OperatorContext;
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
import java.time.ZoneOffset;
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
    private final CustomerRepository customerRepository;
    private final OperatorRepository operatorRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerContainerRepository customerContainerRepository;
    private final AdminRepository adminRepository;

    private final OrderCalculationService orderCalculationService;
    private final CartPriceCalculationService cartPriceCalculationService;
    private final PromoService promoService;
    private final CampaignService campaignService;
    private final InventoryService inventoryService;
    private final ContainerManagementService containerManagementService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final CustomerPackageOrderService customerPackageOrderService;

    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderDetailFactory orderDetailFactory;
    private final OrderMapper orderMapper;
    private final OrderCompletionService orderCompletionService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Uğurla Qeydə alındı",
            message = "'Sifarişiniz #' + #result.orderNumber + ' uğurla qeydə alındı'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse createOrderByCustomer(String phoneNumber, CreateOrderByCustomerRequest request) {
        log.info("Customer creating order - phoneNumber: {}", phoneNumber);

        Customer customer = customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Customer not found with phoneNumber: " + phoneNumber));

        log.info("Customer found - customerId: {}, name: {}", customer.getId(), customer.getFirstName());
        OrderResponse response = createOrderInternal(customer, null, request.getAddressId(), request.getDeliveryDate(),
                request.getItems(), request.getPromoCode(), request.getNote());

        List<Operator> operatorsToNotify = getOperatorsToNotifyForOrder(response.getId());

        if (!operatorsToNotify.isEmpty()) {
            List<NotificationRequest> operatorNotifications = operatorsToNotify.stream()
                    .map(operator -> NotificationRequest.builder()
                            .receiverType(ReceiverType.OPERATOR)
                            .receiverId(operator.getId())
                            .notificationType(NotificationType.ORDER)
                            .title("Yeni Sifariş Daxil Oldu")
                            .message("Yeni sifariş #" + response.getOrderNumber() + " - " +
                                    customer.getFirstName() + " " + customer.getLastName())
                            .referenceId(response.getId())
                            .build())
                    .collect(Collectors.toList());

            notificationService.createNotificationsBatch(operatorNotifications);

            log.info("Notified {} operators about new order (SYSTEM operators + relevant company operators)",
                    operatorsToNotify.size());
        }

        return response;
    }

    @Override
    @Transactional
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Yaradıldı",
            message = "'Sifarişiniz #' + #result.orderNumber + ' uğurla qeydə alındı'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse createOrderByOperator(String operatorEmail, CreateOrderByOperatorRequest request) {
        log.info("Operator creating order - operatorEmail: {}, customerId: {}", operatorEmail, request.getCustomerId());

        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator not found with email: " + operatorEmail));

        if (OperatorContext.isSupplierOperator()) {
            for (CartItem item : request.getItems()) {
                validateProductAccess(item.getProductId());
            }
            log.info("Supplier operator validated - all products belong to company: {}",
                    OperatorContext.getCurrentCompanyId());
        }

        Customer customer = findCustomerById(request.getCustomerId());
        return createOrderInternal(customer, operator, request.getAddressId(), request.getDeliveryDate(),
                request.getItems(), request.getPromoCode(), request.getNotes());
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest updateRequest) {
        log.info("Updating order {}", orderId);

        Order order = findOrderById(orderId);
        validateOrderAccess(order);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Order must be PENDING to be updated. Current status: " + order.getOrderStatus()
            );
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidOrderStateException(
                    "Cannot modify order after payment is completed. " +
                            "Please cancel this order and create a new one, or contact support."
            );
        }

        if (order.getPaymentStatus() == PaymentStatus.PROCESSING ||
                order.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
            throw new InvalidOrderStateException(
                    "Cannot modify order while payment is being processed. " +
                            "Please wait for payment to complete or cancel."
            );
        }

        boolean needsRecalculation = false;

        if (updateRequest.getNotes() != null) {
            order.setNotes(updateRequest.getNotes());
        }

        if (updateRequest.getDeliveryDate() != null) {
            order.setDeliveryDate(updateRequest.getDeliveryDate());
        }

        if (updateRequest.getAddressId() != null) {
            Address newAddress = findCustomerAddress(
                    order.getCustomer().getId(),
                    updateRequest.getAddressId());
            order.setAddress(newAddress);
        }

        if (updateRequest.getItems() != null && !updateRequest.getItems().isEmpty()) {
            if (OperatorContext.isSupplierOperator()) {
                for (UpdateOrderItemRequest item : updateRequest.getItems()) {
                    OrderDetail detail = order.getOrderDetails().stream()
                            .filter(d -> d.getId().equals(item.getOrderDetailId()))
                            .findFirst()
                            .orElseThrow(() -> new NotFoundException("Order detail not found"));
                    validateProductAccess(detail.getProduct().getId());
                }
            }

            updateOrderItems(order, updateRequest.getItems());
            needsRecalculation = true;
        }

        if (needsRecalculation) {
            recalculateOrder(order);

            Map<Long, Integer> productQuantities = order.getOrderDetails().stream()
                    .collect(Collectors.toMap(
                            detail -> detail.getProduct().getId(),
                            OrderDetail::getCount,
                            Integer::sum
                    ));

            inventoryService.validateStockAvailability(productQuantities);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order updated successfully: {}", orderId);

        return orderMapper.toResponse(savedOrder);
    }

    public String getOrderModificationGuidance(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return String.format(
                    """
                            Sifariş #%s ödənilib (%s AZN). Dəyişiklik etmək üçün:
                            1. Bu sifarişi ləğv edin (pul geri qaytarılacaq)
                            2. İstədiyiniz dəyişikliklərlə yeni sifariş yaradın
                            3. Müştəri yeni ödəniş etsin
                            
                            Və ya müştəri ilə əlaqə saxlayaraq dəyişikliklərin qəbul olunduğunu təsdiqləyin.""",
                    order.getOrderNumber(),
                    order.getTotalAmount()
            );
        }

        return "Sifariş normal şəkildə yenilənə bilər.";
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Getting order {}", id);
        Order order = findOrderById(id);
        validateOrderAccess(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    @SendNotification(
            receiverType = ReceiverType.DRIVER,
            notificationType = NotificationType.ORDER,
            title = "Yeni Çatdırılma Tapşırığı",
            message = "'Sizə ' + #result.orderNumber + ' nömrəli sifariş təyin edildi'",
            evaluateMessage = true,
            receiverIdExpression = "#result.driverId",
            referenceIdExpression = "#result.id"
    )
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
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Təsdiqləndi",
            message = "'Sifarişiniz #' + #result.orderNumber + ' təsdiqləndi və tezliklə çatdırılacaq'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse approveOrder(String operatorEmail, Long orderId) {
        log.info("Approving order ID: {}", orderId);
        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new UnauthorizedOperationException("Operator is not active with email: " + operatorEmail);
        }
        Order order = findOrderById(orderId);

        validateOrderAccess(order);

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

        User user = userRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("User " + operatorEmail + " not found"));

        if (order.getStockReservationType() == StockReservationType.SOFT) {
            inventoryService.convertSoftToHardReservation(productQuantities, user);
            order.setStockReservationType(StockReservationType.HARD);
            order.setStockReservationExpiresAt(null);
        } else {
            inventoryService.validateAndReserveStockBatch(productQuantities, user);
            order.setStockReservationType(StockReservationType.HARD);
        }

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
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Ləğv Edildi",
            message = "'Sifarişiniz #' + #result.orderNumber + ' ləğv edildi'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse rejectOrderByCustomer(String phoneNumber, Long orderId, String reason) {
        log.info("Customer {} rejecting order ID: {}", phoneNumber, orderId);

        Order order = findOrderById(orderId);

        Customer customer = customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Customer " + phoneNumber + " not found"));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new UnauthorizedOperationException("Customer " + customer.getId() + " cannot reject order " + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Customer can only reject PENDING orders");
        }

        promoService.releasePromoUsageByOrder(orderId);
        campaignService.releaseCampaignUsageByOrder(orderId);

        refundPayment(order);

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        Order savedOrder = orderRepository.save(order);

        List<Operator> operatorsToNotify = getOperatorsToNotifyForOrder(savedOrder.getId());
        if (!operatorsToNotify.isEmpty()) {
            List<NotificationRequest> operatorNotifications = operatorsToNotify.stream()
                    .map(op -> NotificationRequest.builder()
                            .receiverType(ReceiverType.OPERATOR)
                            .receiverId(op.getId())
                            .notificationType(NotificationType.ORDER)
                            .title("Müştəri Sifarişi Ləğv Etdi")
                            .message("Sifariş #" + savedOrder.getOrderNumber() + " müştəri " +
                                    customer.getFirstName() + " tərəfindən ləğv edildi. Səbəb: " +
                                    (reason != null ? reason : "Səbəb göstərilməyib"))
                            .referenceId(savedOrder.getId())
                            .build())
                    .collect(Collectors.toList());

            notificationService.createNotificationsBatch(operatorNotifications);
            log.info("Notified {} operators about order cancellation", operatorsToNotify.size());
        }

        log.info("Order {} rejected by customer {}", orderId, customer.getId());
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Rədd Edildi",
            message = "'Sifarişiniz #' + #result.orderNumber + ' rədd edildi'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse rejectOrderByOperator(String operatorEmail, Long orderId, String reason) {
        log.info("Operator {} rejecting order ID: {} with reason: {}", operatorEmail, orderId, reason);

        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator " + operatorEmail + " not found"));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE) {
            throw new UnauthorizedOperationException("Operator is not active with email: " + operatorEmail);
        }

        Order order = findOrderById(orderId);
        validateOrderAccess(order);

        OrderResponse response = executeOrderRejection(order, operator, reason);
        log.info("Order {} rejected by operator {}", orderId, operatorEmail);
        return response;
    }

    @Override
    @Transactional
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Rədd Edildi",
            message = "'Sifarişiniz #' + #result.orderNumber + ' rədd edildi'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse rejectOrderByAdmin(String adminEmail, Long orderId, String reason) {
        log.info("Admin {} rejecting order ID: {} with reason: {}", adminEmail, orderId, reason);

        adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new NotFoundException("Admin " + adminEmail + " not found"));

        Order order = findOrderById(orderId);

        // Admins can reject any order regardless of assigned operator/branch
        OrderResponse response = executeOrderRejection(order, null, reason);
        log.info("Order {} rejected by admin {}", orderId, adminEmail);
        return response;
    }

    @Override
    @Transactional
    @SendNotification(
            receiverType = ReceiverType.CUSTOMER,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Çatdırıldı",
            message = "'Sifarişiniz #' + #result.orderNumber + ' çatdırıldı. Təşəkkür edirik!'",
            evaluateMessage = true,
            receiverIdExpression = "#result.customerId",
            referenceIdExpression = "#result.id"
    )
    @SendNotification(
            receiverType = ReceiverType.OPERATOR,
            notificationType = NotificationType.ORDER,
            title = "Sifariş Tamamlandı",
            message = "'Sifariş #' + #result.orderNumber + ' sürücü tərəfindən tamamlandı'",
            evaluateMessage = true,
            receiverIdExpression = "#result.operatorId",
            referenceIdExpression = "#result.id"
    )
    public OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest) {
        log.info("Completing order {}", orderId);

        Order order = findOrderById(orderId);

        Order completedOrder = orderCompletionService.completeOrder(order, completeDeliveryRequest);

        log.info("Order {} completed successfully", completedOrder.getOrderNumber());
        return orderMapper.toResponse(completedOrder);
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
        Page<Order> orderPage;

        if (OperatorContext.isSupplierOperator()) {
            Long companyId = OperatorContext.getCurrentCompanyId();
            log.info("Supplier operator requesting pending orders - filtering by company ID: {}", companyId);
            orderPage = orderRepository.findByOrderStatusAndCompanyId(OrderStatus.PENDING, companyId, pageable);
        } else {
            log.info("System operator requesting pending orders - returning all");
            orderPage = orderRepository.findByOrderStatus(OrderStatus.PENDING, pageable);
        }

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, orderPage);
    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrdersForManagement(Pageable pageable) {
        log.info("Getting all orders for management with page: {}", pageable);

        Page<Order> orderPage;

        if (OperatorContext.isSupplierOperator()) {
            Long companyId = OperatorContext.getCurrentCompanyId();
            log.info("Supplier operator requesting all orders - filtering by company ID: {}", companyId);
            orderPage = orderRepository.findByCompanyId(companyId, pageable);
        } else {
            log.info("System operator requesting all orders - returning all");
            orderPage = orderRepository.findAll(pageable);
        }

        List<Order> orders = orderPage.getContent();

        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return PageResponse.of(responses, orderPage);
    }

    @Override
    public PageResponse<OrderResponse> getAllOrdersByCustomer(Pageable pageable, String phoneNumber) {
        log.info("Fetching all orders for customer phoneNumber {}", phoneNumber);

        Customer customer = customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Customer not found with phoneNumber: " + phoneNumber));

        Page<Order> orderPage = orderRepository.findByCustomerId(customer.getId(), pageable);

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

        validateOrderAccess(order);

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(
                    "Order must be APPROVED to get driver collection info. Current state: "
                            + order.getOrderStatus());
        }

        Map<Long, Integer> customerContainerBalances = new HashMap<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            Optional<CustomerContainer> containerOpt = customerContainerRepository
                    .findByCustomerIdAndProductId(order.getCustomer().getId(), detail.getProduct().getId());

            int available = containerOpt.map(CustomerContainer::getQuantity).orElse(0);
            customerContainerBalances.put(detail.getProduct().getId(), available);
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
            int customerHas = customerContainerBalances.getOrDefault(detail.getProduct().getId(), 0);
            int shortfall = Math.max(0, expectedToCollect - customerHas);

            BigDecimal extraDepositPerBottle = detail.getDepositPerUnit();
            BigDecimal totalExtraDeposit = extraDepositPerBottle
                    .multiply(BigDecimal.valueOf(shortfall))
                    .setScale(2, RoundingMode.HALF_UP);

            String warningMessage = null;
            if (shortfall > 0) {
                hasInsufficientContainers = true;
                totalPotentialExtraDeposit = totalPotentialExtraDeposit.add(totalExtraDeposit);
                warningMessage = String.format(
                        "Customer only has %d/%d containers. Collect extra %s AZN if missing!",
                        customerHas, expectedToCollect, totalExtraDeposit
                );
            }

            expectations.add(BottleCollectionExpectation.builder()
                    .productId(detail.getProduct().getId())
                    .productName(detail.getProduct().getName())
                    .expectedToCollect(expectedToCollect)
                    .customerHasAvailable(customerHas)
                    .shortfall(shortfall)
                    .extraDepositPerBottle(extraDepositPerBottle)
                    .totalExtraDeposit(totalExtraDeposit)
                    .warningMessage(warningMessage)
                    .build());
        }

        String collectionWarning = null;
        if (hasInsufficientContainers) {
            collectionWarning = String.format(
                    """
                            ATTENTION: Customer doesn't have all expected containers! " +
                                        "If customer cannot return missing containers, collect EXTRA %s AZN deposit. " +
                                        "Original amount: %s → Possible amount: %s""",
                    totalPotentialExtraDeposit,
                    order.getTotalAmount(),
                    order.getTotalAmount().add(totalPotentialExtraDeposit)
            );
        }
        return DriverCollectionInfoResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .customerPhone(order.getCustomer().getUser().getPhoneNumber())
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

    private Customer findCustomerById(Long customerId) {
        return customerRepository.findByIdAndIsActiveTrue(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + customerId));
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order Not Found with id: " + id));
    }

    private Address findCustomerAddress(Long customerId, Long addressId) {
        return addressRepository
                .findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() ->
                        new NotFoundException("Address not found with id: " + addressId + " for customer: " + customerId));
    }

    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver Not Found with id " + id));
    }

    private Campaign findCampaignByCampaignCode(String campaignCode) {
        return campaignRepository.findByCampaignCode(campaignCode)
                .orElseThrow(() -> new NotFoundException(
                        "Campaign not found: " + campaignCode));
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
    }

    private OrderResponse createOrderInternal(
            Customer customer,
            Operator operator,
            Long addressId,
            LocalDate deliveryDate,
            List<CartItem> items,
            String promoCode,
            String notes) {

        log.info("Creating order for customer ID: {}, operator: {}, items: {}",
                customer.getId(),
                operator != null ? operator.getId() : "none",
                items.size());

        Address address = findCustomerAddress(customer.getId(), addressId);

        CalculatePriceRequest priceRequest = new CalculatePriceRequest();
        priceRequest.setCustomerId(customer.getId());
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
                        CartItem::getQuantity,
                        Integer::sum
                ));


        ContainerDepositSummary depositSummary =
                containerManagementService.calculateAvailableContainerRefunds(
                        customer.getId(),
                        productQuantities
                );

        String orderNumber = orderNumberGenerator.generateOrderNumber();
        log.info("Generated order number: {}", orderNumber);

        Order order = new Order();
        order.setCustomer(customer);
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

        order.setStockReservationType(StockReservationType.SOFT);
        order.setStockReservedAt(LocalDateTime.now(ZoneOffset.UTC));
        order.setStockReservationExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));

        order.setSubtotal(pricing.getSubtotal());
        order.setTotalDepositCharged(pricing.getTotalDepositCharged());
        order.setTotalDepositRefunded(pricing.getTotalDepositRefunded());
        order.setNetDeposit(pricing.getNetDeposit());
        order.setTotalItems(pricing.getTotalItems());
        order.setPromoDiscount(pricing.getPromoDiscount() != null
                ? pricing.getPromoDiscount()
                : BigDecimal.ZERO);
        order.setCampaignDiscount(BigDecimal.ZERO);

        BigDecimal amount = order.getSubtotal()
                .subtract(order.getPromoDiscount())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount
                .add(order.getNetDeposit())
                .setScale(2, RoundingMode.HALF_UP);

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

        boolean promoApplied = false;
        if (promoCode != null
                && !promoCode.trim().isEmpty()
                && Boolean.TRUE.equals(pricing.getPromoValid())) {
            try {
                ApplyPromoResponse promoResult = promoService.applyPromo(
                        ApplyPromoRequest.builder()
                                .customerId(customer.getId())
                                .orderId(savedOrder.getId())
                                .promoCode(promoCode)
                                .orderAmount(pricing.getSubtotal())
                                .build()
                );

                if (Boolean.TRUE.equals(promoResult.getSuccess())) {
                    savedOrder.setPromo(promoService.getPromoEntityByCode(promoCode));
                    promoApplied = true;
                    log.info("Promo applied: {}, Discount: {}",
                            promoCode,
                            pricing.getPromoDiscount());
                }
            } catch (Exception e) {
                log.error("Failed to apply promo: {}", promoCode, e);
            }
        }

        CampaignApplicationResult campaignResult = applyCampaigns(
                savedOrder,
                customer,
                productQuantities,
                promoApplied,
                savedOrder.getSubtotal()
        );

        Map<Long, Integer> allProducts = new HashMap<>(productQuantities);

        for (OrderCampaignBonus bonus : savedOrder.getCampaignBonuses()) {
            if (bonus.getProduct() != null && bonus.getQuantity() > 0) {
                allProducts.merge(
                        bonus.getProduct().getId(),
                        bonus.getQuantity(),
                        Integer::sum
                );
            }
        }

        inventoryService.softReserveStockBatch(allProducts);

        log.info("Soft reserved stock for {} regular products + {} campaign bonuses = {} total products",
                productQuantities.size(),
                savedOrder.getCampaignBonuses().size(),
                allProducts.size());

        if (campaignResult.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            savedOrder.setCampaignDiscount(campaignResult.getTotalDiscount());

            BigDecimal finalAmount = savedOrder.getSubtotal()
                    .subtract(savedOrder.getPromoDiscount())
                    .subtract(campaignResult.getTotalDiscount())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal finalTotal = finalAmount
                    .add(savedOrder.getNetDeposit())
                    .setScale(2, RoundingMode.HALF_UP);

            savedOrder.setAmount(finalAmount);
            savedOrder.setTotalAmount(finalTotal);

            log.info("Campaign discount applied: {}, New total: {}",
                    campaignResult.getTotalDiscount(),
                    finalTotal);
        }

        containerManagementService.reserveContainersForOrder(savedOrder, depositSummary);

        log.info("Reserved {} containers from customer balance",
                depositSummary.getTotalContainersUsed());

        Order finalOrder = orderRepository.save(savedOrder);

        log.info("Order created successfully: {} - Subtotal: {}, Promo: {}, Campaign: {}, Deposits: {}, Total: {}",
                finalOrder.getOrderNumber(),
                finalOrder.getSubtotal(),
                finalOrder.getPromoDiscount(),
                campaignResult.getTotalDiscount(),
                finalOrder.getNetDeposit(),
                finalOrder.getTotalAmount());

        return orderMapper.toResponse(finalOrder);
    }

    private CampaignApplicationResult applyCampaigns(
            Order order,
            Customer customer,
            Map<Long, Integer> productQuantities,
            boolean willUsePromo,
            BigDecimal orderTotal) {

        CampaignApplicationResult result = new CampaignApplicationResult();

        GetEligibleCampaignsRequest campaignRequest = GetEligibleCampaignsRequest.builder()
                .customerId(customer.getId())
                .productQuantities(productQuantities)
                .willUsePromoCode(willUsePromo)
                .orderTotal(orderTotal)
                .build();

        EligibleCampaignsResponse eligibleCampaigns =
                campaignService.getEligibleCampaigns(campaignRequest);

        List<EligibleCampaignInfo> campaignsToApply = eligibleCampaigns
                .getEligibleCampaigns()
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getWillBeApplied()))
                .toList();

        if (campaignsToApply.isEmpty()) {
            log.debug("No campaigns to apply for order");
            return result;
        }

        log.info("Applying {} campaigns to order", campaignsToApply.size());

        for (EligibleCampaignInfo campaignInfo : campaignsToApply) {
            try {
                ApplyCampaignRequest applyCampaignRequest = ApplyCampaignRequest.builder()
                        .campaignCode(campaignInfo.getCampaignCode())
                        .customerId(customer.getId())
                        .orderId(order.getId())
                        .build();

                ApplyCampaignResponse campaignResponse =
                        campaignService.applyCampaign(applyCampaignRequest);

                if (Boolean.TRUE.equals(campaignResponse.getSuccess())) {
                    processCampaignByType(order, campaignInfo, campaignResponse, result);

                    log.info("Campaign applied: {} ({}), Bonus value: {}",
                            campaignResponse.getCampaignName(),
                            campaignInfo.getCampaignType(),
                            campaignResponse.getBonusValue());
                }
            } catch (Exception e) {
                log.error("Error applying campaign: {}", campaignInfo.getCampaignCode(), e);
            }
        }

        log.info("Total campaigns applied: {}, Total discount: {}",
                result.getAppliedCampaignsCount(),
                result.getTotalDiscount());

        return result;
    }

    private void processCampaignByType(
            Order order,
            EligibleCampaignInfo campaignInfo,
            ApplyCampaignResponse campaignResponse,
            CampaignApplicationResult result
    ) {
        switch (campaignInfo.getCampaignType()) {
            case BUY_X_GET_Y_FREE -> processBuyXGetYFreeCampaign(order, campaignResponse, result);
            case BUY_X_PAY_FOR_Y -> processBuyXPayForYCampaign(order, campaignResponse, result);
            case FIRST_ORDER_BONUS -> processFirstOrderBonusCampaign(order, campaignResponse, result);
            case LOYALTY_BONUS -> processLoyaltyBonusCampaign(order, campaignResponse, result);
            default -> log.warn("Unknown campaign type: {}", campaignInfo.getCampaignType());
        }

        result.incrementAppliedCampaigns();
        result.addDiscount(campaignResponse.getBonusValue());
    }

    private void processBuyXGetYFreeCampaign(
            Order order,
            ApplyCampaignResponse campaignResponse,
            CampaignApplicationResult result
    ) {
        if (campaignResponse.getFreeQuantity() == null || campaignResponse.getFreeQuantity() <= 0) {
            log.warn("No free product for campaign: {}", campaignResponse.getCampaignCode());
            return;
        }

        Product freeProduct = findProductById(campaignResponse.getFreeProductId());

        Campaign campaign = findCampaignByCampaignCode(campaignResponse.getCampaignCode());

        OrderCampaignBonus campaignBonus = new OrderCampaignBonus();
        campaignBonus.setOrder(order);
        campaignBonus.setCampaign(campaign);
        campaignBonus.setProduct(freeProduct);
        campaignBonus.setQuantity(campaignResponse.getFreeQuantity());
        campaignBonus.setBonusValue(campaignResponse.getBonusValue());
        campaignBonus.setBonusType("FREE_PRODUCT");

        order.getCampaignBonuses().add(campaignBonus);

        result.addFreeProduct(freeProduct.getName(), campaignResponse.getFreeQuantity());

        log.info("Added FREE product: {} x {} - value: {}",
                freeProduct.getName(),
                campaignResponse.getFreeQuantity(),
                campaignResponse.getBonusValue());
    }

    private void processBuyXPayForYCampaign(
            Order order,
            ApplyCampaignResponse campaignResponse,
            CampaignApplicationResult result
    ) {
        if (campaignResponse.getFreeQuantity() == null || campaignResponse.getFreeQuantity() <= 0) {
            log.warn("No discounted quantity for campaign: {}", campaignResponse.getCampaignCode());
            return;
        }

        Product discountedProduct = findProductById(campaignResponse.getFreeProductId());

        Campaign campaign = findCampaignByCampaignCode(campaignResponse.getCampaignCode());

        OrderCampaignBonus campaignBonus = new OrderCampaignBonus();
        campaignBonus.setOrder(order);
        campaignBonus.setCampaign(campaign);
        campaignBonus.setProduct(discountedProduct);
        campaignBonus.setQuantity(campaignResponse.getFreeQuantity());
        campaignBonus.setBonusValue(campaignResponse.getBonusValue());
        campaignBonus.setBonusType("DISCOUNTED_PRODUCT");

        order.getCampaignBonuses().add(campaignBonus);

        result.addDiscountedProduct(discountedProduct.getName(), campaignResponse.getFreeQuantity());

        log.info("Added DISCOUNT for product: {} x {} - discount: {}",
                discountedProduct.getName(),
                campaignResponse.getFreeQuantity(),
                campaignResponse.getBonusValue());
    }

    private void processFirstOrderBonusCampaign(
            Order order,
            ApplyCampaignResponse campaignResponse,
            CampaignApplicationResult result
    ) {
        Campaign campaign = findCampaignByCampaignCode(campaignResponse.getCampaignCode());

        OrderCampaignBonus campaignBonus = new OrderCampaignBonus();
        campaignBonus.setOrder(order);
        campaignBonus.setCampaign(campaign);
        campaignBonus.setProduct(null);
        campaignBonus.setQuantity(0);
        campaignBonus.setBonusValue(campaignResponse.getBonusValue());
        campaignBonus.setBonusType("FIRST_ORDER_BONUS");

        order.getCampaignBonuses().add(campaignBonus);

        result.addBonusDiscount("First Order Bonus", campaignResponse.getBonusValue());

        log.info("Added FIRST ORDER BONUS - discount: {}", campaignResponse.getBonusValue());
    }

    private void processLoyaltyBonusCampaign(
            Order order,
            ApplyCampaignResponse campaignResponse,
            CampaignApplicationResult result
    ) {
        Campaign campaign = findCampaignByCampaignCode(campaignResponse.getCampaignCode());

        OrderCampaignBonus campaignBonus = new OrderCampaignBonus();
        campaignBonus.setOrder(order);
        campaignBonus.setCampaign(campaign);
        campaignBonus.setProduct(null);
        campaignBonus.setQuantity(0);
        campaignBonus.setBonusValue(campaignResponse.getBonusValue());
        campaignBonus.setBonusType("LOYALTY_BONUS");

        order.getCampaignBonuses().add(campaignBonus);

        result.addBonusDiscount("Loyalty Bonus", campaignResponse.getBonusValue());

        log.info("Added LOYALTY BONUS - discount: {}", campaignResponse.getBonusValue());
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
                        order.getCustomer().getId(), productQuantities
                );

        applyContainerInfoToOrderDetails(order.getOrderDetails(), depositSummary);
        orderCalculationService.recalculateOrderFinancials(order);
        order.setEmptyBottlesExpected(depositSummary.getTotalContainersUsed());

        log.info("Order recalculated: subtotal={}, netDeposit={}, total={}",
                order.getSubtotal(), order.getNetDeposit(), order.getTotalAmount());
    }

    private void refundPayment(Order order) {

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS &&
                order.getPaymentMethod() == PaymentMethod.CARD) {

            try {
                log.info("Order {} has successful CARD payment, initiating refund", order.getId());
                paymentService.refundPayment(order.getId());
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Refund processed successfully for order {}", order.getId());

            } catch (Exception e) {
                log.error("Failed to refund payment for order {}", order.getId(), e);
                throw new PaymentRefundException("Could not process refund: " + e.getMessage(), e);
            }

        } else if (order.getPaymentStatus() == PaymentStatus.SUCCESS &&
                order.getPaymentMethod() == PaymentMethod.CASH) {

            log.info("Order {} was paid with CASH, no online refund needed", order.getId());
            order.setPaymentStatus(PaymentStatus.CANCELLED);

        } else {
            log.info("Order {} payment status is {}, no refund action needed",
                    order.getId(), order.getPaymentStatus());
        }
    }

    private void validateProductAccess(Long productId) {
        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            Product product = findProductById(productId);

            if (!product.getCompany().getId().equals(operatorCompanyId)) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to create orders with products from other companies"
                );
            }
        }
    }

    private void validateOrderAccess(Order order) {
        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();

            boolean hasCompanyProducts = order.getOrderDetails().stream()
                    .anyMatch(detail -> detail.getProduct().getCompany().getId().equals(operatorCompanyId));
            if (!hasCompanyProducts) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to access this order. It doesn't contain products from your company."
                );
            }
        }
    }

    private List<Operator> getOperatorsToNotifyForOrder(Long orderId) {
        Order order = findOrderById(orderId);

        List<Long> companyIds = order.getOrderDetails().stream()
                .map(detail -> detail.getProduct().getCompany().getId())
                .distinct()
                .toList();

        if (companyIds.isEmpty()) {
            log.warn("order {} has no products, notifying only SYSTEM operators", orderId);
            return operatorRepository.findByOperatorStatusAndOperatorType(
                    OperatorStatus.ACTIVE, OperatorType.SYSTEM
            );
        }

        List<Operator> operators = operatorRepository.findOperatorsToNotify(
                OperatorStatus.ACTIVE, companyIds
        );

        log.info("Order {} contains products from {} companies. Notifying {} operators (SYSTEM + company operators)",
                orderId, companyIds.size(), operators.size());

        return operators;
    }

    private OrderResponse executeOrderRejection(Order order, Operator operator, String reason) {

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException("Order must be PENDING or APPROVED to reject");
        }

        promoService.releasePromoUsageByOrder(order.getId());
        campaignService.releaseCampaignUsageByOrder(order.getId());
        refundPayment(order);

        if (order.getOrderStatus() == OrderStatus.APPROVED) {
            log.info("Order was APPROVED, releasing warehouse stock for order ID: {}", order.getId());

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
                log.debug("Released {} units of product {} back to warehouse", entry.getValue(), entry.getKey());
            }

            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                product.setOrderCount(Math.max(0, product.getOrderCount() - detail.getCount()));
                productRepository.save(product);
            }
        }

        order.setOrderStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);

        if (operator != null) {
            order.setOperator(operator);
        }

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }
}