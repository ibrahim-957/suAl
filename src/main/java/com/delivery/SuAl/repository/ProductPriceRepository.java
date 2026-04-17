package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {

    @Query("SELECT pp FROM ProductPrice pp WHERE pp.product.id = :productId AND pp.validTo IS NULL")
    Optional<ProductPrice> findActiveByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE ProductPrice pp SET pp.validTo = :closedAt WHERE pp.product.id = :productId AND pp.validTo IS NULL")
    int closeActivePrice(@Param("productId") Long productId, @Param("closedAt") LocalDateTime closedAt);

    @Query("SELECT pp FROM ProductPrice pp WHERE pp.product.id = :productId ORDER BY pp.validFrom DESC")
    List<ProductPrice> findHistoryByProductId(@Param("productId") Long productId);

    boolean existsByProductIdAndValidToIsNull(Long productId);

    @Query("SELECT p FROM ProductPrice p " +
            "WHERE p.product.id IN :productIds AND p.validTo IS NULL")
    List<ProductPrice> findActiveByProductIdIn(List<Long> productIds);
}
