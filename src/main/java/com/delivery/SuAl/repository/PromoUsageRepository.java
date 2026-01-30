package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PromoUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromoUsageRepository extends JpaRepository<PromoUsage, Long> {
    @Query("SELECT COUNT(pu) FROM PromoUsage pu " +
            "WHERE pu.customer.id = :customerId AND pu.promo.id = :promoId")
    Integer countUsagesByCustomerAndPromo(@Param("customerId") Long customerId, @Param("promoId") Long promoId);
}