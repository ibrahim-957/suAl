package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.exception.BusinessRuleViolationException;
import com.delivery.SuAl.model.enums.NotificationType;
import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;
import com.delivery.SuAl.repository.CustomerContainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderValidationService {
    private final CustomerContainerRepository customerContainerRepository;
    private final NotificationService notificationService;

    public void validateCollectedBottles(
            Order order,
            List<BottleCollectionItem> bottlesCollected
    ){
        if (bottlesCollected == null || bottlesCollected.isEmpty()) {
            log.info("No bottles collected for order {}", order.getOrderNumber());

            for (OrderDetail detail : order.getOrderDetails()) {
                detail.setContainersReturned(0);
            }
            return;
        }

        log.info("Validating {} bottle collection items for order {}",
                bottlesCollected.size(), order.getOrderNumber());

        Map<Long, OrderDetail> orderDetailMap = order.getOrderDetails().stream()
                .collect(Collectors.toMap(
                        detail -> detail.getProduct().getId(),
                        detail -> detail
                ));

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (BottleCollectionItem item : bottlesCollected) {
            OrderDetail detail = orderDetailMap.get(item.getProductId());
            if (detail == null) {
                errors.add(String.format(
                        "Product %d was not in this order", item.getProductId()
                ));
                continue;
            }

            if (item.getQuantity() < 0) {
                errors.add(String.format(
                        "Cannot collect negative bottles: %d for product %d",
                        item.getQuantity(), item.getProductId()
                ));
                continue;
            }

            if (item.getQuantity() > detail.getCount()) {
                int extraBottles = item.getQuantity() - detail.getCount();

                int customerBalance = getCustomerContainerBalance(
                        order.getCustomer().getId(),
                        item.getProductId()
                );

                if (item.getQuantity() > customerBalance) {
                    errors.add(String.format(
                            "Product %d: Cannot collect %d bottles. " +
                                    "Customer balance: %d, Delivered now: %d",
                            item.getProductId(), item.getQuantity(),
                            customerBalance, detail.getCount()
                    ));
                    continue;
                }

                warnings.add(String.format(
                        "Product %d (%s): Collected %d extra bottles from previous orders. " +
                                "Delivered: %d, Collected: %d, Customer balance: %d",
                        item.getProductId(),
                        detail.getProduct().getName(),
                        extraBottles,
                        detail.getCount(),
                        item.getQuantity(),
                        customerBalance
                ));

                log.warn("Order {}: Collecting {} extra bottles of product {} " +
                                "(delivered: {}, collected: {}, customer balance: {})",
                        order.getOrderNumber(), extraBottles, item.getProductId(),
                        detail.getCount(), item.getQuantity(), customerBalance);
            }

            if (item.getQuantity() > detail.getCount() * 10){
                errors.add(String.format(
                        "Suspicious quantity for product %d: collecting %d but only delivered %d. " +
                                "Please verify this is not a typo.",
                        item.getProductId(), item.getQuantity(), detail.getCount()
                ));
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Bottle collection validation failed: " + String.join("; ", errors)
            );
        }

        if (!warnings.isEmpty() && order.getOperator() != null) {
            String warningMessage = String.join("\n", warnings);

            notificationService.createNotification(NotificationRequest.builder()
                            .receiverType(ReceiverType.OPERATOR)
                            .receiverId(order.getOperator().getId())
                            .notificationType(NotificationType.ORDER)
                            .title("Əlavə qablar toplandı")
                            .message(String.format(
                                    "Sifariş %s: Sürücü əvvəlki sifarişlərdən qalan qabları topladı.\n\n%s",
                                    order.getOrderNumber(),
                                    warningMessage
                            ))
                            .referenceId(order.getId())
                            .build()
            );

        }
    }

    private int getCustomerContainerBalance(Long customerId, Long productId){
        return customerContainerRepository
                .findByCustomerIdAndProductId(customerId, productId)
                .map(CustomerContainer::getQuantity)
                .orElse(0);
    }
}
