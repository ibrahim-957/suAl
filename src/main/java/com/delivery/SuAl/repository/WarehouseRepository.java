package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByProductId(Long productId);

    List<Warehouse> findByCompanyId(Long companyId);

    @Query("SELECT w FROM Warehouse w " +
            "WHERE w.fullCount < w.minimumStockAlert " +
            "ORDER BY w.fullCount ASC")
    List<Warehouse> findLowStockProducts();

    @Query("SELECT w FROM Warehouse w " +
            "WHERE w.fullCount = 0")
    List<Warehouse> findOutOfStockProducts();

    @Query("SELECT SUM(w.fullCount) FROM Warehouse w")
    Long getTotalInventory();

    @Query("SELECT w FROM Warehouse w " +
            "WHERE w.emptyCount > 0 " +
            "ORDER BY w.emptyCount DESC ")
    List<Warehouse> findByProductsWithEmpties();
}
