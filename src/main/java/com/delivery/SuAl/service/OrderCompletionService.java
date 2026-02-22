package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.exception.InvalidOrderStateException;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCompletionService {
    private final OrderRepository orderRepository;
    private final ContainerManagementService containerManagementService;
    private final OrderCalculationService orderCalculationService;
    private final CustomerPackageOrderService customerPackageOrderService;
    private final OrderValidationService orderValidationService;

    @Transactional
    public Order completeOrder(Order order, CompleteDeliveryRequest request){
        log.info("Completing order {}",  order.getOrderNumber());

        validateOrderCanBeCompleted(order);

        orderValidationService.validateCollectedBottles(order, request.getBottlesCollected());

        if (order.isPackageOrder()){
            processPackageOrderCompletion(order, request);
        } else {
            processRegularOrderCompletion(order, request);
        }

        finalizeOrderCompletion(order, request);

        Order completedOrder = orderRepository.save(order);

        log.info("Order {} completed successfully", completedOrder.getOrderNumber());

        return completedOrder;
    }

    private void validateOrderCanBeCompleted(Order order) {
        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(
                    String.format("Order must be APPROVED to complete. Current status: %s",
                            order.getOrderStatus())
            );
        }

        if (order.getDriver() == null) {
            throw new InvalidOrderStateException(
                    "Driver must be assigned before completing order"
            );
        }

        log.debug("Order {} validation passed", order.getOrderNumber());
    }

    private void processRegularOrderCompletion(Order order, CompleteDeliveryRequest request) {
        log.info("Processing REGULAR order completion for order {}", order.getOrderNumber());

        containerManagementService.processOrderCompletion(
                order.getCustomer().getId(),
                order.getOrderDetails(),
                request.getBottlesCollected()
        );

        int totalExpected = order.getEmptyBottlesExpected();
        int totalCollected = calculateTotalBottlesCollected(order);
        int missingBottles = totalExpected - totalCollected;

        log.info("Container collection - Expected: {}, Collected: {}, Missing: {}",
                totalExpected, totalCollected, missingBottles);

        orderCalculationService.recalculateDepositsFromActualCollection(order);

        if (missingBottles > 0) {
            handleMissingContainers(order, missingBottles);
        }

        order.setEmptyBottlesCollected(totalCollected);
    }

    private void processPackageOrderCompletion(Order order, CompleteDeliveryRequest request) {
        log.info("Processing PACKAGE order completion for order {}", order.getOrderNumber());

        CustomerPackageOrder packageOrder = order.getPackageOrder();

        if (!containerManagementService.validatePackageContainerAvailability(
                packageOrder.getCustomer().getId(),
                packageOrder.getAffordablePackage().getId(),
                packageOrder.getFrequency())) {
            throw new InvalidOrderStateException(
                    "Customer does not have enough containers for this package order"
            );
        }

        containerManagementService.processPackageOrderCompletion(packageOrder);

        log.info("Package order completed: {} bottles delivered",
                packageOrder.getAffordablePackage().getTotalContainers() * packageOrder.getFrequency());
    }

    private void finalizeOrderCompletion(Order order, CompleteDeliveryRequest request){
        log.info("Finalizing order completion for {}", order.getOrderNumber());

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));

        if (order.getPaymentMethod() == PaymentMethod.CASH &&
                order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setPaidAt(LocalDateTime.now(ZoneOffset.UTC));
            log.info("Cash payment finalized for order {}", order.getOrderNumber());
        }

        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            appendNotes(order, request.getNotes());
        }

        if (order.getPackageOrder() != null) {
            customerPackageOrderService.updatePackageOrderStatus(
                    order.getPackageOrder().getId()
            );
            log.info("Updated package order status for package {}",
                    order.getPackageOrder().getId());
        }
    }

    private void handleMissingContainers(Order order, int missingBottles){
        BigDecimal depositPerUnit = order.getOrderDetails().isEmpty()
                ? BigDecimal.ZERO
                : order.getOrderDetails().getFirst().getDepositPerUnit();

        BigDecimal extraDepositCharge = depositPerUnit
                .multiply(BigDecimal.valueOf(missingBottles))
                .setScale(2, RoundingMode.HALF_UP);

        log.warn("CUSTOMER DID NOT RETURN {} CONTAINERS - Extra deposit kept: {} ({}×{})",
                missingBottles, extraDepositCharge, missingBottles, depositPerUnit);

        String missingContainerNote = String.format(
                "Missing containers: %d bottles not returned. Deposit not refunded: %s AZN (%s × %d)",
                missingBottles, extraDepositCharge, depositPerUnit, missingBottles
        );

        appendNotes(order, missingContainerNote);

        log.info("Deposit kept for missing containers: {}", extraDepositCharge);
    }

    private int calculateTotalBottlesCollected(Order order){
        return order.getOrderDetails().stream()
                .mapToInt(OrderDetail::getContainersReturned)
                .sum();
    }

    private void appendNotes(Order order, String  newNotes){
        if (newNotes == null || newNotes.trim().isEmpty()){
            return;
        }

        String existingNotes = order.getNotes();
        if (existingNotes != null && !existingNotes.trim().isEmpty()){
            order.setNotes(existingNotes + " /// " + newNotes);
        } else {
            order.setNotes(newNotes);
        }
    }
}
