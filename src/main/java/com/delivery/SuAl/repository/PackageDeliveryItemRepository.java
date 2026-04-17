package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PackageDeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageDeliveryItemRepository extends JpaRepository<PackageDeliveryItem, Long> {
    @Query("SELECT pdi FROM PackageDeliveryItem pdi " +
            "WHERE pdi.distribution.id = :distributionId")
    List<PackageDeliveryItem> findByDistributionId(@Param("distributionId") Long distributionId);

}
