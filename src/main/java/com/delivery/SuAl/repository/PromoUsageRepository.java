package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PromoUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromoUsageRepository extends JpaRepository<PromoUsage, Long> {
    @Query("SELECT COUNT(pu) FROM PromoUsage pu " +
            "WHERE pu.customer.id = :customerId AND pu.promo.id = :promoId")
    Integer countUsagesByCustomerAndPromo(@Param("customerId") Long customerId, @Param("promoId") Long promoId);

    List<PromoUsage> findByOrderId(Long orderId);

    @Modifying
    @Query("DELETE FROM PromoUsage pu WHERE pu.order.id = :orderId")
    int deleteByOrderId(@Param("orderId") Long orderId);
}