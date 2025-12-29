package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PromoUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromoUsageRepository extends JpaRepository<PromoUsage, Long> {
    List<PromoUsage> findByUserId(Long userId);

    Page<PromoUsage> findByUserId(Long userId, Pageable pageable);

    List<PromoUsage> findByPromoId(Long promoId);

    Page<PromoUsage> findByPromoId(Long promoId, Pageable pageable);

    Long countByUserIdAndPromoId(Long userId, Long promoId);

    @Query("SELECT COUNT(pu) FROM PromoUsage pu " +
            "WHERE pu.user.id = :userId AND pu.promo.id = :promoId")
    Integer countUsagesByUserAndPromo(@Param("userId")  Long userId, @Param("promoId") Long promoId);
}