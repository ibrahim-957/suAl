package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.ContainerCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContainerCollectionRepository  extends JpaRepository<ContainerCollection, Long> {
    Page<ContainerCollection> findByWarehouseId(long warehouseId, Pageable pageable);

    Page<ContainerCollection> findByProductId(long productId, Pageable pageable);

    List<ContainerCollection> findByCollectionDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT cc FROM ContainerCollection cc WHERE cc.warehouse.id = :warehouseId " +
            "AND cc.collectionDateTime BETWEEN :startDate AND :endDate")
    List<ContainerCollection> findByWarehouseAndDateRange(
            @Param("warehouseId") Long warehouseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT SUM(cc.emptyContainers) FROM ContainerCollection cc " +
            "WHERE cc.warehouse.id = :warehouseId AND cc.product.id = :productId")
    Integer getTotalEmptyContainersByWarehouseAndProduct(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId
    );

    @Query("SELECT SUM(cc.damagedContainers) FROM ContainerCollection cc " +
            "WHERE cc.warehouse.id = :warehouseId AND cc.product.id = :productId")
    Integer getTotalDamagedContainersByWarehouseAndProduct(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId
    );
}
