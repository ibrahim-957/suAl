package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
    @Query("""
            SELECT sb FROM StockBatch sb
            WHERE sb.product.id = :productId
              AND sb.warehouse.id = :warehouseId
              AND sb.remainingQuantity > 0
            ORDER BY sb.createdAt ASC
            """)
    List<StockBatch> findAvailableBatchesFifo(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    @Query("""
            SELECT COALESCE(SUM(sb.remainingQuantity), 0)
            FROM StockBatch sb
            WHERE sb.product.id = :productId
              AND sb.warehouse.id = :warehouseId
            """)
    Integer sumRemainingQuantity(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    List<StockBatch> findByInvoiceItemId(Long purchaseInvoiceItemId);

    @Query("""
            SELECT sb FROM StockBatch sb
            WHERE sb.product.id = :productId
              AND sb.warehouse.id = :warehouseId
              AND sb.remainingQuantity = 0
            ORDER BY sb.createdAt ASC
            """)
    List<StockBatch> findExhaustedBatches(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);
}
