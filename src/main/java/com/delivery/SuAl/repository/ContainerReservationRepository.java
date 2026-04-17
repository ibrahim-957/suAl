package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.ContainerReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContainerReservationRepository extends JpaRepository<ContainerReservation, Long> {
    List<ContainerReservation> findByOrderId(Long orderId);

    @Query("SELECT cr FROM ContainerReservation cr " +
            "WHERE cr.customer.id = :customerId " +
            "AND cr.product.id = :productId " +
            "AND cr.released = false " +
            "AND (cr.expiresAt IS NULL OR cr.expiresAt > :now)")
    List<ContainerReservation> findActiveReservations(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT COALESCE(SUM(cr.quantityReserved), 0) " +
            "FROM ContainerReservation cr " +
            "WHERE cr.customer.id = :customerId " +
            "AND cr.product.id = :productId " +
            "AND cr.released = false " +
            "AND (cr.expiresAt IS NULL OR cr.expiresAt > :now)")
    Integer sumReservedQuantity(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT cr FROM ContainerReservation cr " +
            "WHERE cr.released = false " +
            "AND cr.expiresAt IS NOT NULL " +
            "AND cr.expiresAt < :now")
    List<ContainerReservation> findExpiredReservations(@Param("now") LocalDateTime now);

    void deleteByOrderId(Long orderId);
}
