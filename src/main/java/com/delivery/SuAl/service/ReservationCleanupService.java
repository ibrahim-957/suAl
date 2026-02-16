package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.ContainerReservation;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.StockReservationType;
import com.delivery.SuAl.repository.ContainerReservationRepository;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationCleanupService {
    private final ContainerReservationRepository containerReservationRepository;
    private final OrderRepository orderRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredContainerReservations() {
        log.info("Starting expired container reservation cleanup");

        LocalDateTime now = LocalDateTime.now();
        List<ContainerReservation> expiredReservations =
                containerReservationRepository.findExpiredReservations(now);

        if (expiredReservations.isEmpty()) {
            log.info("No expired container reservations found");
            return;
        }

        log.info("Found {} expired container reservations", expiredReservations.size());

        for (ContainerReservation reservation : expiredReservations) {
            reservation.setReleased(true);
            reservation.setReleasedAt(now);

            log.debug("Released expired container reservation: orderId={}, productId={}, quantity={}",
                    reservation.getOrder() != null ? reservation.getOrder().getId() : "null",
                    reservation.getProductId(),
                    reservation.getQuantityReserved());
        }

        containerReservationRepository.saveAll(expiredReservations);

        log.info("Successfully released {} expired container reservations",
                expiredReservations.size());
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupExpiredStockReservations() {
        log.info("Starting expired stock reservation cleanup");

        LocalDateTime now = LocalDateTime.now();

        List<Order> expiredOrders = orderRepository.findExpiredStockReservations(
                StockReservationType.SOFT,
                OrderStatus.PENDING,
                now
        );

        if (expiredOrders.isEmpty()) {
            log.info("No expired stock reservations found");
            return;
        }

        log.info("Found {} orders with expired stock reservations", expiredOrders.size());

        for (Order order : expiredOrders) {
            log.warn("Order {} has expired stock reservation (expires at: {}). " +
                            "Consider cancelling or following up with customer.",
                    order.getOrderNumber(),
                    order.getStockReservationExpiresAt());
        }

        log.info("Logged {} orders with expired stock reservations", expiredOrders.size());
    }
}
