package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.StockMovement;
import com.delivery.SuAl.model.enums.MovementType;
import com.delivery.SuAl.model.enums.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdAndWarehouseIdOrderByCreatedAtDesc(Long productId, Long warehouseId);

    List<StockMovement> findByReferenceTypeAndReferenceId(ReferenceType referenceType, Long referenceId);

    @Query("SELECT sm FROM StockMovement sm " +
            "WHERE sm.movementType = :type " +
            "AND sm.createdAt BETWEEN :from AND :to " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> finsByTypeInDateRange(
            @Param("type")MovementType type,
            @Param("from")LocalDateTime from,
            @Param("to")LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(
                CASE WHEN sm.movementType IN ('PURCHASE', 'TRANSFER_IN', 'RETURN_FROM_CUSTOMER')
                     THEN sm.quantity
                     ELSE -sm.quantity END
            ), 0)
            FROM StockMovement sm
            WHERE sm.product.id = :productId
              AND sm.warehouse.id = :warehouseId
            """)
    Integer calculateNetStock(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    boolean existsByReferenceTypeAndReferenceId(ReferenceType referenceType, Long referenceId);
}
