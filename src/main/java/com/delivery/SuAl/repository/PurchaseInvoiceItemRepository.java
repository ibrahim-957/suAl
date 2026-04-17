package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.PurchaseInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceItemRepository extends JpaRepository<PurchaseInvoiceItem, Long> {
    List<PurchaseInvoiceItem> findByInvoiceId(Long purchaseInvoiceId);

    List<PurchaseInvoiceItem> findByProductId(Long productId);


    @Query("""
            SELECT pii.product.id, SUM(pii.quantity)
            FROM PurchaseInvoiceItem pii
            WHERE pii.invoice.status = 'APPROVED'
              AND pii.product.id = :productId
            GROUP BY pii.product.id
            """)
    Optional<Object[]> sumApprovedQuantityByProductId(@Param("productId") Long productId);

    void deleteByInvoiceId(Long purchaseInvoiceId);
}
