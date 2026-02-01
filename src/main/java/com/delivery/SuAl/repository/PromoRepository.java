package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Promo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoRepository extends JpaRepository<Promo, Long> {
    Optional<Promo> findByPromoCode(String promoCode);

    @Query("SELECT  p FROM Promo p " +
            "WHERE p.promoStatus = 'ACTIVE' " +
            "AND (p.validFrom IS NULL OR p.validFrom <= CURRENT_DATE) " +
            "AND (p.validTo IS NULL OR p.validTo >= CURRENT_DATE)")
    List<Promo> findActivePromos();

    boolean existsByPromoCode(String promoCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promo p WHERE p.promoCode = :promoCode")
    Optional<Promo> findByPromoCodeWithLock(@Param("promoCode") String promoCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promo p WHERE p.id = :id")
    Optional<Promo> findByIdWithLock(@Param("id") Long id);
}
