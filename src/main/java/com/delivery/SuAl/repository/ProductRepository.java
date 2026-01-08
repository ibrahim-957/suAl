package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    List<Product> findByProductStatus(ProductStatus productStatus);

    List<Product> findByCompanyId(Long companyId);

    List<Product> findByCompanyIdAndProductStatus(Long companyId, ProductStatus productStatus);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCategoryIdAndProductStatus(Long categoryId, ProductStatus productStatus);

    List<Product> findBySize(String size);

    @Query("SELECT p FROM Product p " +
            "WHERE p.productStatus = 'ACTIVE' " +
            "ORDER BY p.company.name, p.name")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.prices " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithPrices(@Param("id") Long id);

    Optional<Product> findByNameAndCompanyId(String name, Long companyId);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.company.id = :companyId")
    Long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.category.id = :categoryId")
    Long countByCategoryId(@Param("categoryId")  Long categoryId);

}
