package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.AffordablePackageProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AffordablePackageProductRepository extends JpaRepository<AffordablePackageProduct, Long> {
    @Query("SELECT app FROM AffordablePackageProduct app " +
            "WHERE app.affordablePackage.id = :packageId")
    List<AffordablePackageProduct> findByPackageId(@Param("packageId") Long packageId);

    @Query("SELECT app FROM AffordablePackageProduct app " +
            "JOIN FETCH app.product " +
            "WHERE app.affordablePackage.id = :packageId")
    List<AffordablePackageProduct> findByPackageIdWithProduct(@Param("packageId") Long packageId);
}
