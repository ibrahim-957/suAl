package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    Optional<ProductSize> findByLabel(String label);
    boolean existsByLabel(String label);
    List<ProductSize> findAllByIsActiveTrue();
}
