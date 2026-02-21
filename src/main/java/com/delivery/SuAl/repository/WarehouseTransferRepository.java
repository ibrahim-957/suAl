package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.WarehouseTransfer;
import com.delivery.SuAl.model.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseTransferRepository extends JpaRepository<WarehouseTransfer, Long> {
    List<WarehouseTransfer> findByStatus(TransferStatus status);

    List<WarehouseTransfer> findByFromWarehouseId(Long warehouseId);

    List<WarehouseTransfer> findByToWarehouseId(Long warehouseId);

    @Query("""
            SELECT wt FROM WarehouseTransfer wt
            WHERE wt.fromWarehouse.id = :warehouseId
               OR wt.toWarehouse.id = :warehouseId
            ORDER BY wt.createdAt DESC
            """)
    List<WarehouseTransfer> findAllInvolvingWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("""
            SELECT wt FROM WarehouseTransfer wt
            LEFT JOIN FETCH wt.items i
            LEFT JOIN FETCH i.product
            WHERE wt.id = :id
            """)
    Optional<WarehouseTransfer> findByIdWithItems(@Param("id") Long id);

    List<WarehouseTransfer> findByStatusAndFromWarehouseId(TransferStatus status, Long fromWarehouseId);

}
