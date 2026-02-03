package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {
    Optional<Product> findByNameAndCompanyId(String name, Long companyId);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.company.id = :companyId")
    Long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.category.id = :categoryId")
    Long countByCategoryId(@Param("categoryId")  Long categoryId);

    Page<Product> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "WHERE p.id = :productId AND p.company.id = :companyId")
    Optional<Product> findByIdAndCompanyId(@Param("productId") Long productId, @Param("companyId") Long companyId);
}
