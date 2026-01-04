package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Basket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BasketRepository extends JpaRepository<Basket, Long> {
    Optional<Basket> findByUserId(Long userId);

    @Query("SELECT b FROM Basket b LEFT JOIN FETCH b.basketItems " +
            "WHERE b.user.id = :userId")
    Optional<Basket> findByUserIdWithItems(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
