package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    @Query("SELECT p FROM Price p WHERE p.product.id = :productId")
    Optional<Price> findByProductId(@Param("productId") Long productId);

    List<Price> findByCompanyId(Long companyId);

    @Query("SELECT p FROM Price p " +
            "WHERE p.product.id = :productId " +
            "AND p.company.id = :companyId")
    Optional<Price> findByProductIdAndCompanyId(@Param("productId") Long productId,
                                                @Param("companyId") Long companyId);

    @Query("SELECT p FROM Price p " +
            "ORDER BY p.updatedAt DESC")
    List<Price> findLatestPrices();

    @Query("SELECT p FROM Price p WHERE p.product.id IN :productIds")
    List<Price> findAllByProductIdIn(@Param("productIds") List<Long> productIds);
}
