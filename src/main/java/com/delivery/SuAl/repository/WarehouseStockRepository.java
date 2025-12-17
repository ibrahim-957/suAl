package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.WarehouseStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findByWarehouseId(Long warehouseId);

    Optional<WarehouseStock> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<WarehouseStock> findByProductId(Long productId);

    @Query("SELECT ws FROM WarehouseStock ws " +
            "WHERE ws.fullCount < ws.minimumStockAlert")
    List<WarehouseStock> findLowStockProducts();

    @Query("SELECT ws FROM WarehouseStock ws " +
            "WHERE ws.fullCount = 0")
    List<WarehouseStock> findOutOfStockProducts();

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
