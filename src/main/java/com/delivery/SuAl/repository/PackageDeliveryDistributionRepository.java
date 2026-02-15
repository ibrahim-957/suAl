package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PackageDeliveryDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageDeliveryDistributionRepository extends JpaRepository<PackageDeliveryDistribution, Long> {
    @Query("SELECT pdd FROM PackageDeliveryDistribution pdd " +
            "WHERE pdd.packageOrder.id = :packageOrderId " +
            "ORDER BY pdd.deliveryNumber ASC")
    List<PackageDeliveryDistribution> findByPackageOrderIdOrderByDeliveryNumber(@Param("packageOrderId") Long packageOrderId);

    @Query("SELECT pdd FROM PackageDeliveryDistribution pdd " +
            "JOIN FETCH pdd.deliveryItems " +
            "WHERE pdd.packageOrder.id = :packageOrderId " +
            "ORDER BY pdd.deliveryNumber ASC")
    List<PackageDeliveryDistribution> findByPackageOrderIdWithItems(@Param("packageOrderId") Long packageOrderId);

}
