package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.*;
import com.delivery.SuAl.exception.BusinessRuleViolationException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.OrderDepositInfo;
import com.delivery.SuAl.helper.PackageDepositSummary;
import com.delivery.SuAl.mapper.CustomerPackageOrderMapper;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.enums.*;
import com.delivery.SuAl.model.request.affordablepackage.DeliveryDistributionRequest;
import com.delivery.SuAl.model.request.affordablepackage.DeliveryProductRequest;
import com.delivery.SuAl.model.request.affordablepackage.OrderAffordablePackageRequest;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.response.affordablepackage.CustomerPackageOrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.*;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerPackageOrderServiceImpl implements CustomerPackageOrderService {
    private final CustomerPackageOrderRepository customerPackageOrderRepository;
    private final AffordablePackageRepository affordablePackageRepository;
    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PackageDeliveryDistributionRepository distributionRepository;
    private final ProductPriceRepository priceRepository;
    private final PackageDepositCalculationService depositCalculationService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final CustomerPackageOrderMapper packageOrderMapper;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final OperatorRepository operatorRepository;
    private final ContainerManagementService containerManagementService;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public CustomerPackageOrderResponse orderPackage(Long customerId, OrderAffordablePackageRequest request) {
        log.info("Customer {} ordering package {}", customerId, request.getPackageId());

        String orderMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        //validateOnePackagePerMonth(customerId, orderMonth);

        AffordablePackage affordablePackage = affordablePackageRepository
                .findByIdAndNotDeleted(request.getPackageId())
                .orElseThrow(() -> new NotFoundException("Package not found"));

        if (!affordablePackage.getIsActive()) {
            throw new BusinessRuleViolationException("Package is not active.");
        }

        if (!affordablePackage.isFrequencyValid(request.getFrequency())) {
            String errorMessage = affordablePackage.getFrequencyErrorMessage(request.getFrequency());
            log.warn("Invalid frequency for package {}: {}", request.getPackageId(), errorMessage);
            throw new BusinessRuleViolationException(errorMessage);
        }

        Customer customer = customerRepository.findByIdAndIsActiveTrue(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        validateDistribution(request, affordablePackage);

        Map<Long, Integer> packageProducts = getPackageProductsMap(affordablePackage);
        PackageDepositSummary depositSummary =
                depositCalculationService.calculatePackageDeposits(customerId, packageProducts);

        CustomerPackageOrder packageOrder = createCustomerPackageOrder(
                customer, affordablePackage, request, depositSummary, orderMonth
        );

        List<PackageDeliveryDistribution> distributions =
                createDeliveryDistributions(packageOrder, request.getDistributions());
        packageOrder.setDeliveryDistributions(distributions);

        List<Order> generatedOrders = generateOrdersFromPackage(
                packageOrder, request.getDistributions(), depositSummary
        );

        orderRepository.saveAll(generatedOrders);
        packageOrder.setGeneratedOrders(generatedOrders);

        if (!generatedOrders.isEmpty()) {
            Order firstOrder = generatedOrders.getFirst();

            if (depositSummary.getOldContainersToCollect() > 0){
                Map<Long, Integer> firstOrderProducts = firstOrder.getOrderDetails().stream()
                        .collect(Collectors.toMap(
                                detail -> detail.getProduct().getId(),
                                OrderDetail::getCount,
                                Integer::sum
                        ));

                ContainerDepositSummary firstOrderDepositSummary =
                        containerManagementService.calculateAvailableContainerRefunds(
                                customerId, firstOrderProducts
                        );

                containerManagementService.reserveContainersForOrder(
                        firstOrder, firstOrderDepositSummary
                );

                log.info("Reserved {} containers for first package order delivery",
                        firstOrderDepositSummary.getTotalContainersUsed());
            }
        }

        packageOrder.setPaymentStatus(PaymentStatus.PENDING);

        CustomerPackageOrder saved = customerPackageOrderRepository.save(packageOrder);

        log.info("Package order created: {} - Total: {} AZN, Payment method: {}, Payment status: {}",
                saved.getOrderNumber(), saved.getTotalPrice(),
                saved.getPaymentMethod(), saved.getPaymentStatus());

        return mapToResponse(saved);
    }

    @Transactional
    public PaymentDTO initializePackagePayment(Long packageOrderId, String language) {
        log.info("Initializing payment for package order {}", packageOrderId);

        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package order not found"));

        if (packageOrder.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessRuleViolationException("Package order already paid");
        }

        if (packageOrder.getPaymentMethod() != PaymentMethod.CARD) {
            throw new BusinessRuleViolationException("Payment method must be CARD for online payment");
        }

        if (packageOrder.getOrderStatus() == PackageOrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Cannot pay for cancelled package order");
        }

        Order firstOrder = packageOrder.getGeneratedOrders().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No orders found for package"));

        CreatePaymentDTO paymentDTO = CreatePaymentDTO.builder()
                .orderId(firstOrder.getId())
                .amount(packageOrder.getTotalPrice())
                .description("Package Order: " + packageOrder.getOrderNumber())
                .language(language != null ? language : "az")
                .build();

        PaymentDTO payment = paymentService.initialize(paymentDTO);

        log.info("Payment initialized for package order {}. Reference: {}",
                packageOrder.getOrderNumber(), payment.getReferenceId());

        return payment;
    }

    @Transactional
    public void handlePaymentSuccess(Long packageOrderId, String transactionId) {
        log.info("Processing payment success for package order {}", packageOrderId);

        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package order not found"));

        if (packageOrder.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.warn("Package order {} already marked as paid", packageOrderId);
            return;
        }

        packageOrder.setPaymentStatus(PaymentStatus.SUCCESS);

        for (Order order : packageOrder.getGeneratedOrders()) {
            if (order.getPaymentStatus() == PaymentStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setPaidAt(LocalDateTime.now());
            }
        }

        customerPackageOrderRepository.save(packageOrder);

        log.info("Payment success processed for package order {}", packageOrderId);
    }

    @Transactional
    public void handlePaymentFailure(Long packageOrderId, String reason) {
        log.warn("Processing payment failure for package order {}: {}", packageOrderId, reason);

        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package order not found"));

        packageOrder.setPaymentStatus(PaymentStatus.FAILED);
        packageOrder.setOrderStatus(PackageOrderStatus.CANCELLED);
        packageOrder.setCancelledAt(LocalDateTime.now());

        for (Order order : packageOrder.getGeneratedOrders()) {
            if (order.getOrderStatus() == OrderStatus.PENDING) {
                order.setOrderStatus(OrderStatus.REJECTED);
                order.setRejectionReason("Payment failed: " + reason);
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
        }

        customerPackageOrderRepository.save(packageOrder);

        log.info("Payment failure processed for package order {}", packageOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerPackageOrderResponse> getCustomerPackageOrders(
            Long customerId,
            Pageable pageable
    ) {
        Page<CustomerPackageOrder> orders = customerPackageOrderRepository
                .findByCustomerId(customerId, pageable);

        List<CustomerPackageOrderResponse> responses = orders.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, orders);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPackageOrderResponse getPackageOrderById(Long packageOrderId) {
        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package order not found"));
        return mapToResponse(packageOrder);
    }

    @Override
    @Transactional
    public CustomerPackageOrderResponse cancelPackageOrder(Long customerId, Long packageOrderId) {
        log.info("Customer {} requesting to cancel package order {}", customerId, packageOrderId);

        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package order not found"));

        if (!packageOrder.getCustomer().getId().equals(customerId)) {
            throw new BusinessRuleViolationException("Unauthorized to cancel this package");
        }

        if (packageOrder.isCancelled()) {
            throw new BusinessRuleViolationException("Package is already cancelled");
        }

        if (packageOrder.isCompleted()) {
            throw new BusinessRuleViolationException("Cannot cancel completed package");
        }

        long completedDeliveries = packageOrder.getGeneratedOrders().stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .count();

        if (completedDeliveries > 0) {
            throw new BusinessRuleViolationException(
                    "Cannot cancel package after deliveries have started. " +
                    "Please contact customer service at [phone number] for assistance."
            );
        }

        log.info("No deliveries completed yet. Proceeding with package cancellation.");

        for (Order order : packageOrder.getGeneratedOrders()) {
            if (order.getOrderStatus() == OrderStatus.PENDING ||
                    order.getOrderStatus() == OrderStatus.APPROVED) {
                boolean wasApproved = order.getOrderStatus() == OrderStatus.APPROVED;

                order.setOrderStatus(OrderStatus.REJECTED);
                order.setRejectionReason("Package cancelled by customer before first delivery");

                if (wasApproved) {
                    log.info("Releasing stock for approved order {} in cancelled package",
                            order.getOrderNumber());

                    Map<Long, Integer> productQuantities = order.getOrderDetails().stream()
                            .collect(Collectors.toMap(
                                    detail -> detail.getProduct().getId(),
                                    OrderDetail::getCount,
                                    Integer::sum
                            ));

                    inventoryService.releaseStockBatch(productQuantities);
                }

                containerManagementService.releaseContainerReservations(order.getId());
            }
        }

        packageOrder.setOrderStatus(PackageOrderStatus.CANCELLED);
        packageOrder.setCancelledAt(LocalDateTime.now());

        if (packageOrder.getPaymentStatus() == PaymentStatus.SUCCESS &&
                packageOrder.getPaymentMethod() == PaymentMethod.CARD) {

            try {
                log.info("Initiating refund for cancelled package order {}", packageOrderId);

                Order firstOrder = packageOrder.getGeneratedOrders().stream()
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("No orders found for package"));

                paymentService.refundPayment(firstOrder.getId());

                packageOrder.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Refund processed successfully for package order {}", packageOrderId);

            } catch (Exception e) {
                log.error("Failed to process refund for package order {}", packageOrderId, e);
                packageOrder.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            }
        }

        CustomerPackageOrder saved = customerPackageOrderRepository.save(packageOrder);
        log.info("Package order {} cancelled successfully (before first delivery)", packageOrderId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CustomerPackageOrderResponse toggleAutoRenew(Long customerId, Long packageOrderId, Boolean autoRenew) {
        log.info("Customer {} toggling auto-renew for package order {} to {}",
                customerId, packageOrderId, autoRenew);

        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package order not found"));

        if (!packageOrder.getCustomer().getId().equals(customerId)) {
            throw new BusinessRuleViolationException("Unauthorized to modify this package");
        }

        packageOrder.setAutoRenew(autoRenew);
        CustomerPackageOrder saved = customerPackageOrderRepository.save(packageOrder);

        log.info("Auto-renew toggled to {} for package order {}", autoRenew, packageOrderId);
        return mapToResponse(saved);
    }


    @Override
    @Transactional
    public void processAutoRenewals(String previousMonth) {
        log.info("Processing auto-renewals for month {}", previousMonth);

        List<CustomerPackageOrder> completedPackages =
                customerPackageOrderRepository.findCompletedPackagesForAutoRenewal(previousMonth);

        log.info("Found {} packages eligible for auto-renewal", completedPackages.size());

        int successCount = 0;
        int failureCount = 0;

        for (CustomerPackageOrder oldPackage : completedPackages) {
            try {
                log.info("Auto-renewing package order {} for customer {}",
                        oldPackage.getId(), oldPackage.getCustomer().getId());

                OrderAffordablePackageRequest renewalRequest = buildRenewalRequest(oldPackage);

                CustomerPackageOrderResponse newPackage = orderPackage(
                        oldPackage.getCustomer().getId(),
                        renewalRequest
                );

                log.info("Successfully auto-renewed package. Old: {}, New: {}",
                        oldPackage.getOrderNumber(), newPackage.getOrderNumber());

                sendAutoRenewalSuccessNotification(oldPackage, newPackage.getOrderNumber());

                successCount++;

            } catch (Exception ex) {
                log.error("Failed to auto-renew package {} for customer {}. Error: {}",
                        oldPackage.getId(), oldPackage.getCustomer().getId(), ex.getMessage(), ex);
                failureCount++;

                sendAutoRenewalFailureNotifications(oldPackage, ex.getMessage());
            }
        }

        log.info("Auto-renewal completed. Success: {}, Failures: {}", successCount, failureCount);

        if (failureCount > 0) {
            sendAutoRenewalSummaryNotification(successCount, failureCount, previousMonth);
        }
    }


    private void sendAutoRenewalSuccessNotification(CustomerPackageOrder oldPackage, String newOrderNumber) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .receiverType(ReceiverType.CUSTOMER)
                    .receiverId(oldPackage.getCustomer().getId())
                    .notificationType(NotificationType.PACKAGE_ORDER)
                    .title("Paket Sifarişi Avtomatik Yeniləndi")
                    .message(String.format(
                            "Paket sifarişiniz #%s uğurla yeniləndi. Yeni sifariş nömrəsi: #%s",
                            oldPackage.getOrderNumber(),
                            newOrderNumber
                    ))
                    .referenceId(oldPackage.getId())
                    .build();

            notificationService.createNotification(notification);

            log.info("Sent auto-renewal success notification to customer {}",
                    oldPackage.getCustomer().getId());

        } catch (Exception e) {
            log.error("Failed to send auto-renewal success notification for package {}",
                    oldPackage.getId(), e);
        }
    }


    private void sendAutoRenewalFailureNotifications(CustomerPackageOrder oldPackage, String errorMessage) {
        try {
            Customer customer = oldPackage.getCustomer();

            String customerTitle;
            String customerMessage;

            if (errorMessage != null && errorMessage.toLowerCase().contains("container")) {
                customerTitle = "Kifayət qədər boş şüşə yoxdur";
                customerMessage = String.format(
                        "Paket sifarişiniz #%s yenilənmədi çünki kifayət qədər boş şüşəniz yoxdur. " +
                                "Zəhmət olmasa şüşələri qaytarın və ya yeni sifariş yaradın.",
                        oldPackage.getOrderNumber()
                );
            } else if (errorMessage != null &&
                    (errorMessage.toLowerCase().contains("payment") ||
                            errorMessage.toLowerCase().contains("card"))) {
                customerTitle = "Ödəniş Uğursuz Oldu";
                customerMessage = String.format(
                        "Paket sifarişiniz #%s yenilənmədi çünki ödəniş uğursuz oldu. " +
                                "Zəhmət olmasa ödəniş metodunuzu yoxlayın və ya müştəri xidməti ilə əlaqə saxlayın.",
                        oldPackage.getOrderNumber()
                );
            } else if (errorMessage != null &&
                    errorMessage.toLowerCase().contains("package") &&
                    errorMessage.toLowerCase().contains("not active")) {
                customerTitle = "Paket Artıq Mövcud Deyil";
                customerMessage = String.format(
                        "Paket sifarişiniz #%s yenilənmədi çünki bu paket artıq mövcud deyil. " +
                                "Zəhmət olmasa digər paketlərimizə baxın.",
                        oldPackage.getOrderNumber()
                );
            } else if (errorMessage != null &&
                    errorMessage.toLowerCase().contains("already have")) {
                customerTitle = "Artıq Aktiv Paket Var";
                customerMessage = String.format(
                        "Paket sifarişiniz #%s yenilənmədi çünki bu ay artıq aktiv paket sifarişiniz var. " +
                                "Növbəti ay avtomatik yenilənəcək.",
                        oldPackage.getOrderNumber()
                );
            } else {
                customerTitle = "Paket Sifarişi Yenilənmədi";
                customerMessage = String.format(
                        "Paket sifarişiniz #%s avtomatik yenilənmədi. " +
                                "Zəhmət olmasa müştəri xidməti ilə əlaqə saxlayın.",
                        oldPackage.getOrderNumber()
                );
            }

            NotificationRequest customerNotification = NotificationRequest.builder()
                    .receiverType(ReceiverType.CUSTOMER)
                    .receiverId(customer.getId())
                    .notificationType(NotificationType.PACKAGE_ORDER)
                    .title(customerTitle)
                    .message(customerMessage)
                    .referenceId(oldPackage.getId())
                    .build();

            notificationService.createNotification(customerNotification);

            log.info("Sent auto-renewal failure notification to customer {} with title: {}",
                    customer.getId(), customerTitle);

            List<Operator> activeOperators = operatorRepository.findByOperatorStatusAndOperatorType(
                    OperatorStatus.ACTIVE,
                    OperatorType.SYSTEM
            );

            if (!activeOperators.isEmpty()) {
                List<NotificationRequest> operatorNotifications = activeOperators.stream()
                        .map(operator -> NotificationRequest.builder()
                                .receiverType(ReceiverType.OPERATOR)
                                .receiverId(operator.getId())
                                .notificationType(NotificationType.PACKAGE_ORDER)
                                .title("Paket Sifarişi Avtomatik Yenilənmədi")
                                .message(String.format(
                                        """
                                                Müştəri: %s %s (ID: %d)
                                                Paket: #%s
                                                Səbəb: %s
                                                Zəhmət olmasa müştəri ilə əlaqə saxlayın.""",
                                        customer.getFirstName(),
                                        customer.getLastName(),
                                        customer.getId(),
                                        oldPackage.getOrderNumber(),
                                        errorMessage != null ? errorMessage : "Naməlum səbəb"
                                ))
                                .referenceId(oldPackage.getId())
                                .build())
                        .collect(Collectors.toList());

                notificationService.createNotificationsBatch(operatorNotifications);

                log.info("Sent auto-renewal failure notifications to {} operators for package {}",
                        activeOperators.size(), oldPackage.getOrderNumber());
            } else {
                log.warn("No active SYSTEM operators found to notify about failed renewal for package {}",
                        oldPackage.getOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to send auto-renewal failure notifications for package {}. Error: {}",
                    oldPackage.getId(), e.getMessage(), e);
        }
    }


    private void sendAutoRenewalSummaryNotification(int successCount, int failureCount, String month) {
        try {
            List<Operator> activeOperators = operatorRepository.findByOperatorStatusAndOperatorType(
                    OperatorStatus.ACTIVE,
                    OperatorType.SYSTEM
            );

            if (activeOperators.isEmpty()) {
                return;
            }

            String summaryMessage = String.format(
                    """
                            Avtomatik Yeniləmə Hesabatı - %s
                            
                             Uğurlu: %d paket
                             Uğursuz: %d paket
                            
                            Uğursuz paketlər üçün müştərilərlə əlaqə saxlayın.""",
                    month,
                    successCount,
                    failureCount
            );

            List<NotificationRequest> summaryNotifications = activeOperators.stream()
                    .map(operator -> NotificationRequest.builder()
                            .receiverType(ReceiverType.OPERATOR)
                            .receiverId(operator.getId())
                            .notificationType(NotificationType.SYSTEM)
                            .title("Avtomatik Yeniləmə Hesabatı")
                            .message(summaryMessage)
                            .build())
                    .collect(Collectors.toList());

            notificationService.createNotificationsBatch(summaryNotifications);

            log.info("Sent auto-renewal summary to {} operators: {} success, {} failures",
                    activeOperators.size(), successCount, failureCount);

        } catch (Exception e) {
            log.error("Failed to send auto-renewal summary notification", e);
        }
    }

    private OrderAffordablePackageRequest buildRenewalRequest(CustomerPackageOrder oldPackage) {
        OrderAffordablePackageRequest request = new OrderAffordablePackageRequest();
        request.setPackageId(oldPackage.getAffordablePackage().getId());
        request.setFrequency(oldPackage.getFrequency());
        request.setPaymentMethod(oldPackage.getPaymentMethod());
        request.setAutoRenew(oldPackage.getAutoRenew());

        List<DeliveryDistributionRequest> distributions = new ArrayList<>();
        for (PackageDeliveryDistribution dist : oldPackage.getDeliveryDistributions()) {
            DeliveryDistributionRequest distReq = new DeliveryDistributionRequest();
            distReq.setDeliveryNumber(dist.getDeliveryNumber());
            distReq.setAddressId(dist.getAddress().getId());

            LocalDate oldDate = dist.getDeliveryDate();
            LocalDate newDate = oldDate.plusMonths(1);
            distReq.setDeliveryDate(newDate);

            List<DeliveryProductRequest> products = new ArrayList<>();
            for (PackageDeliveryItem item : dist.getDeliveryItems()) {
                DeliveryProductRequest prodReq = new DeliveryProductRequest();
                prodReq.setProductId(item.getProduct().getId());
                prodReq.setQuantity(item.getQuantity());
                products.add(prodReq);
            }
            distReq.setProducts(products);

            distributions.add(distReq);
        }

        request.setDistributions(distributions);
        return request;
    }

    @Override
    @Transactional
    public void updatePackageOrderStatus(Long packageOrderId) {
        CustomerPackageOrder packageOrder = customerPackageOrderRepository.findById(packageOrderId)
                .orElseThrow(() -> new NotFoundException("Package Order Not Found"));

        List<Order> generatedOrders = packageOrder.getGeneratedOrders();

        long completedCount = generatedOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED)
                .count();

        if (completedCount > 0 && packageOrder.isPending()) {
            packageOrder.setOrderStatus(PackageOrderStatus.ACTIVE);
            log.info("Package order {} moved to ACTIVE status", packageOrderId);
        }

        if (completedCount == generatedOrders.size()) {
            packageOrder.setOrderStatus(PackageOrderStatus.COMPLETED);
            log.info("Package order {} fully completed", packageOrderId);
        }

        customerPackageOrderRepository.save(packageOrder);
    }

    private void validateOnePackagePerMonth(Long customerId, String orderMonth) {
        long activePackages = customerPackageOrderRepository
                .countActivePackagesForCustomerInMonth(customerId, orderMonth);

        if (activePackages > 0) {
            throw new BusinessRuleViolationException(
                    "You already have an active package this month. Only one package per month allowed."
            );
        }
    }

    private void validateDistribution(
            OrderAffordablePackageRequest request,
            AffordablePackage affordablePackage
    ) {
        if (request.getDistributions() == null || request.getDistributions().isEmpty()) {
            throw new BusinessRuleViolationException("Distributions cannot be empty");
        }

        if (request.getDistributions().size() != request.getFrequency()) {
            throw new BusinessRuleViolationException(
                    String.format("Number of deliveries (%d) must match frequency (%d)",
                            request.getDistributions().size(), request.getFrequency())
            );
        }

        Map<Long, Integer> distributedProducts = new HashMap<>();
        for (DeliveryDistributionRequest dist : request.getDistributions()) {
            for (DeliveryProductRequest prod : dist.getProducts()) {
                distributedProducts.merge(prod.getProductId(), prod.getQuantity(), Integer::sum);
            }
        }

        Map<Long, Integer> packageProducts = getPackageProductsMap(affordablePackage);

        Map<Long, Integer> expectedProducts = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : packageProducts.entrySet()) {
            expectedProducts.put(entry.getKey(), entry.getValue());
        }

        if (!distributedProducts.equals(expectedProducts)) {
            log.error("Product distribution mismatch. Expected: {}, Got: {}",
                    expectedProducts, distributedProducts);
            throw new BusinessRuleViolationException(
                    "Product distribution does not match package contents × frequency"
            );
        }
    }

    private Map<Long, Integer> getPackageProductsMap(AffordablePackage affordablePackage) {
        return affordablePackage.getPackageProducts().stream()
                .collect(Collectors.toMap(
                        pp -> pp.getProduct().getId(),
                        AffordablePackageProduct::getQuantity
                ));
    }

    private CustomerPackageOrder createCustomerPackageOrder(
            Customer customer,
            AffordablePackage affordablePackage,
            OrderAffordablePackageRequest request,
            PackageDepositSummary depositSummary,
            String orderMonth
    ) {
        CustomerPackageOrder packageOrder = new CustomerPackageOrder();
        packageOrder.setCustomer(customer);
        packageOrder.setAffordablePackage(affordablePackage);
        packageOrder.setOrderNumber(orderNumberGenerator.generatePackageOrderNumber());
        packageOrder.setFrequency(request.getFrequency());

        BigDecimal totalPackagePrice = affordablePackage.getTotalPrice()
                .setScale(2, RoundingMode.HALF_UP);

        packageOrder.setPackageProductPrice(totalPackagePrice);
        packageOrder.setTotalContainersInPackage(depositSummary.getTotalContainersInPackage());
        packageOrder.setOldContainersToCollect(depositSummary.getOldContainersToCollect());
        packageOrder.setTotalDepositCharged(depositSummary.getTotalDepositCharged());
        packageOrder.setExpectedDepositRefunded(depositSummary.getExpectedDepositRefund());
        packageOrder.setNetDeposit(depositSummary.getNetDeposit());

        BigDecimal totalPrice = totalPackagePrice
                .add(depositSummary.getNetDeposit())
                .setScale(2, RoundingMode.HALF_UP);
        packageOrder.setTotalPrice(totalPrice);

        packageOrder.setPaymentMethod(request.getPaymentMethod());
        packageOrder.setPaymentStatus(PaymentStatus.PENDING);
        packageOrder.setOrderStatus(PackageOrderStatus.PENDING);
        packageOrder.setOrderMonth(orderMonth);
        packageOrder.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : false);

        return customerPackageOrderRepository.save(packageOrder);
    }

    private List<PackageDeliveryDistribution> createDeliveryDistributions(
            CustomerPackageOrder packageOrder,
            List<DeliveryDistributionRequest> distributions
    ) {
        List<PackageDeliveryDistribution> result = new ArrayList<>();

        for (DeliveryDistributionRequest distReq : distributions) {
            Address address = addressRepository.findById(distReq.getAddressId())
                    .orElseThrow(() -> new NotFoundException(
                            "Address not found with id: " + distReq.getAddressId()));

            PackageDeliveryDistribution distribution = new PackageDeliveryDistribution();
            distribution.setPackageOrder(packageOrder);
            distribution.setDeliveryNumber(distReq.getDeliveryNumber());
            distribution.setDeliveryDate(distReq.getDeliveryDate());
            distribution.setAddress(address);

            PackageDeliveryDistribution saved = distributionRepository.save(distribution);

            List<PackageDeliveryItem> items = new ArrayList<>();
            for (DeliveryProductRequest prodReq : distReq.getProducts()) {
                Product product = productRepository.findById(prodReq.getProductId())
                        .orElseThrow(() -> new NotFoundException(
                                "Product not found with id: " + prodReq.getProductId()));

                PackageDeliveryItem item = new PackageDeliveryItem();
                item.setDistribution(saved);
                item.setProduct(product);
                item.setQuantity(prodReq.getQuantity());
                items.add(item);
            }

            saved.setDeliveryItems(items);
            result.add(saved);
        }

        return result;
    }

    private List<Order> generateOrdersFromPackage(
            CustomerPackageOrder packageOrder,
            List<DeliveryDistributionRequest> distributions,
            PackageDepositSummary packageDepositSummary
    ) {
        List<Order> orders = new ArrayList<>();
        List<OrderDepositInfo> depositInfos = depositCalculationService
                .distributeDepositsAcrossOrders(packageDepositSummary, distributions);

        Map<Long, Integer> allPackageProducts = new HashMap<>();

        for (int i = 0; i < distributions.size(); i++) {
            DeliveryDistributionRequest dist = distributions.get(i);
            OrderDepositInfo depositInfo = depositInfos.get(i);

            Address address = addressRepository.findById(dist.getAddressId())
                    .orElseThrow(() -> new NotFoundException("Address not found"));

            Order order = new Order();
            order.setPackageOrder(packageOrder);
            order.setIsPackageOrder(true);
            order.setDeliveryNumber(i + 1);
            order.setCustomer(packageOrder.getCustomer());
            order.setAddress(address);
            order.setOrderNumber(orderNumberGenerator.generateOrderNumber());
            order.setDeliveryDate(dist.getDeliveryDate());

            BigDecimal productAmount = depositCalculationService.calculateProportionalProductAmount(
                    packageOrder.getPackageProductPrice(),
                    dist.getProducts(),
                    packageOrder.getTotalContainersInPackage()
            );

            order.setSubtotal(productAmount);
            order.setTotalItems(dist.getProducts().stream()
                    .mapToInt(DeliveryProductRequest::getQuantity)
                    .sum());

            order.setTotalDepositCharged(depositInfo.getDepositCharged());
            order.setTotalDepositRefunded(depositInfo.getExpectedDepositRefund());
            order.setNetDeposit(depositInfo.getNetDeposit());
            order.setExpectedDepositRefundedAtCreation(depositInfo.getExpectedDepositRefund());

            if (i == 0) {
                order.setOldContainersToCollect(depositInfo.getContainersToCollect());
            }

            order.setEmptyBottlesExpected(depositInfo.getContainersToCollect());

            BigDecimal totalAmount = productAmount
                    .add(depositInfo.getNetDeposit())
                    .setScale(2, RoundingMode.HALF_UP);

            order.setAmount(productAmount);
            order.setTotalAmount(totalAmount);

            order.setPaymentMethod(packageOrder.getPaymentMethod());

            if (packageOrder.getPaymentMethod() == PaymentMethod.CARD) {
                order.setPaymentStatus(PaymentStatus.PENDING);
            } else {
                order.setPaymentStatus(i == 0 ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
            }

            order.setOrderStatus(OrderStatus.PENDING);

            order.setStockReservationType(StockReservationType.SOFT);
            order.setStockReservedAt(LocalDateTime.now(ZoneOffset.UTC));
            order.setStockReservationExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));

            order.setPromoDiscount(BigDecimal.ZERO);
            order.setCampaignDiscount(BigDecimal.ZERO);

            List<OrderDetail> orderDetails = createOrderDetailsForPackage(order, dist.getProducts());
            order.setOrderDetails(orderDetails);

            orders.add(order);

            for (DeliveryProductRequest prod : dist.getProducts()) {
                allPackageProducts.merge(prod.getProductId(), prod.getQuantity(), Integer::sum);
            }
        }

        inventoryService.softReserveStockBatch(allPackageProducts);

        log.info("Soft reserved stock for package: {} total products across {} deliveries",
                allPackageProducts.size(), distributions.size());

        return orders;
    }

    private List<OrderDetail> createOrderDetailsForPackage(
            Order order,
            List<DeliveryProductRequest> products
    ) {
        List<OrderDetail> details = new ArrayList<>();

        List<Long> productIds = products.stream()
                .map(DeliveryProductRequest::getProductId)
                .toList();

        Map<Long, ProductPrice> priceMap = priceRepository.findActiveByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        price -> price.getProduct().getId(),
                        p -> p,
                        (p1, p2) -> p1.getCreatedAt().isAfter(p2.getCreatedAt()) ? p1 : p2
                ));

        for (DeliveryProductRequest prodReq : products) {
            Product product = productRepository.findById(prodReq.getProductId())
                    .orElseThrow(() -> new NotFoundException(
                            "Product not found with id: " + prodReq.getProductId()));

            ProductPrice activePrice = priceMap.get(product.getId());

            BigDecimal pricePerUnit = activePrice != null
                    ? calculateEffectivePrice(activePrice)
                    : BigDecimal.ZERO;

            // FIX 2: was Price::getBuyPrice — Price entity deleted, use ProductPrice.getBuyPrice()
            BigDecimal buyPrice = activePrice != null
                    ? Optional.ofNullable(activePrice.getBuyPrice()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setCompany(product.getCompany());
            detail.setCategory(product.getCategory());
            detail.setCount(prodReq.getQuantity());
            detail.setPricePerUnit(pricePerUnit);
            detail.setBuyPrice(buyPrice);

            BigDecimal subtotal = pricePerUnit
                    .multiply(BigDecimal.valueOf(prodReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            detail.setSubtotal(subtotal);

            detail.setDepositPerUnit(product.getDepositAmount());

            BigDecimal depositCharged = product.getDepositAmount()
                    .multiply(BigDecimal.valueOf(prodReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            detail.setDepositCharged(depositCharged);

            BigDecimal lineTotal = subtotal
                    .add(depositCharged)
                    .setScale(2, RoundingMode.HALF_UP);
            detail.setLineTotal(lineTotal);

            detail.setContainersReturned(0);
            detail.setDepositRefunded(BigDecimal.ZERO);
            detail.setDeposit(depositCharged);

            details.add(detail);
        }

        return details;
    }

    private CustomerPackageOrderResponse mapToResponse(CustomerPackageOrder packageOrder) {
        return packageOrderMapper.toResponse(packageOrder);
    }

    private BigDecimal calculateEffectivePrice(ProductPrice price){
        if (price.getSellPrice() == null) return BigDecimal.ZERO;
        if (price.getDiscountPercent() == null
                || price.getDiscountPercent().compareTo(BigDecimal.ZERO) == 0) {
            return price.getSellPrice();
        }
        BigDecimal multiplier = BigDecimal.ONE
                .subtract(price.getDiscountPercent()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return price.getSellPrice()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}