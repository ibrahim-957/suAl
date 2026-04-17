package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.WarehouseTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseTransferItemRepository extends JpaRepository<WarehouseTransferItem, Long> {
    List<WarehouseTransferItem> findByTransferId(Long transferId);

    List<WarehouseTransferItem> findByProductId(Long productId);

    void deleteByTransferId(Long transferId);
}
