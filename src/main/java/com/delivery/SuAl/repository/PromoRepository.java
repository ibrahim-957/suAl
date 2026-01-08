package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.model.enums.PromoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoRepository extends JpaRepository<Promo, Long> {
    Optional<Promo> findByPromoCode(String promoCode);

    List<Promo> findByPromoStatus(PromoStatus promoStatus);

    @Query("SELECT  p FROM Promo p " +
            "WHERE p.promoStatus = 'ACTIVE' " +
            "AND (p.validFrom IS NULL OR p.validFrom <= CURRENT_DATE) " +
            "AND (p.validTo IS NULL OR p.validTo >= CURRENT_DATE)")
    List<Promo> findActivePromos();

    @Query("SELECT p FROM Promo p " +
            "WHERE p.promoStatus = 'ACTIVE' AND p.validTo BETWEEN CURRENT_DATE AND :endDate " +
            "ORDER BY p.validTo ASC ")
    List<Promo> findExpiringSoon(@Param("endDate") LocalDate endDate);

    boolean existsByPromoCode(String promoCode);
}
