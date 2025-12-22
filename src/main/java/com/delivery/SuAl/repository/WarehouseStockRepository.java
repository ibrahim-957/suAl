package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.WarehouseStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findByWarehouseId(Long warehouseId);

    Optional<WarehouseStock> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<WarehouseStock> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.product.id = :productId")
    List<WarehouseStock> findByProductIdWithLock(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.product.id IN :productIds")
    List<WarehouseStock> findByProductIdsWithLock(@Param("productIds") List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.id = :id")
    Optional<WarehouseStock> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(ws.fullCount + ws.emptyCount + ws.damagedCount), 0) " +
            "FROM WarehouseStock ws WHERE ws.warehouse.id = :warehouseId")
    Long getTotalInventoryByWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.warehouse.id IN :warehouseIds")
    List<WarehouseStock> findByWarehouseIdIn(@Param("warehouseIds") List<Long> warehouseIds);

    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.warehouse.id = :warehouseId")
    Page<WarehouseStock> findByWarehouseIdPageable(
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);

    @Query("SELECT ws FROM WarehouseStock ws " +
            "WHERE ws.warehouse.id = :warehouseId " +
            "AND ws.fullCount > 0 " +
            "AND ws.fullCount <= ws.minimumStockAlert")
    List<WarehouseStock> findLowStockProductsByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT ws FROM WarehouseStock ws " +
            "WHERE ws.warehouse.id = :warehouseId " +
            "AND ws.fullCount = 0")
    List<WarehouseStock> findOutOfStockProductsByWarehouseId(@Param("warehouseId") Long warehouseId);
}
