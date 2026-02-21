package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByCompanyIdAndNameAndSizeId(Long companyId, String name, Long sizeId);

    Optional<Product> findByIdAndCompanyId(Long productId, Long companyId);

    Page<Product> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.company.id = :companyId")
    Long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p.company.id, COUNT(p) FROM Product p " +
            "WHERE p.company.id IN :companyIds GROUP BY p.company.id")
    List<Object[]> countByCompanyIdGrouped(@Param("companyIds") List<Long> companyIds);

    @Query("SELECT p.category.id, COUNT(p) FROM Product p " +
            "WHERE p.category.id IN :categoryIds GROUP BY p.category.id")
    List<Object[]> countByCategoryIdGrouped(@Param("categoryIds") List<Long> categoryIds);

    @Query("SELECT p FROM Product p WHERE p.productStatus = 'ACTIVE' " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:sizeId IS NULL OR p.size.id = :sizeId) " +
            "AND (:companyId IS NULL OR p.company.id = :companyId)")
    Page<Product> findAllWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("sizeId") Long sizeId,
            @Param("companyId") Long companyId,
            Pageable pageable
    );

}
