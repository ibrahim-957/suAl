package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PurchaseInvoice;
import com.delivery.SuAl.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    List<PurchaseInvoice> findByStatus(InvoiceStatus status);

    List<PurchaseInvoice> findByCompanyId(Long supplierId);

    List<PurchaseInvoice> findByWarehouseId(Long warehouseId);

    List<PurchaseInvoice> findByCompanyIdAndStatus(Long supplierId, InvoiceStatus status);

    @Query("""
            SELECT pi FROM PurchaseInvoice pi
            LEFT JOIN FETCH pi.items i
            LEFT JOIN FETCH i.product
            WHERE pi.id = :id
            """)
    Optional<PurchaseInvoice> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT pi.status FROM PurchaseInvoice pi WHERE pi.id = :id")
    Optional<InvoiceStatus> findStatusById(@Param("id") Long id);
}
