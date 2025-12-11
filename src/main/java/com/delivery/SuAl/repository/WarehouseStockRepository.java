package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("SELECT SUM(ws.fullCount) FROM WarehouseStock ws " +
            "WHERE ws.warehouse.id = :warehouseId")
    Long getTotalInventoryByWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("SELECT ws FROM WarehouseStock ws " +
            "WHERE ws.emptyCount > 0 " +
            "ORDER BY ws.emptyCount DESC")
    List<WarehouseStock> findProductsWithEmpties();
}
