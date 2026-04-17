package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.enums.InvoiceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_invoices", indexes = {
        @Index(name = "idx_purchase_invoice_company", columnList = "company_id"),
        @Index(name = "idx_purchase_invoice_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_purchase_invoice_date", columnList = "invoice_date"),
        @Index(name = "idx_purchase_invoice_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, updatable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;


    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_deposit_amount", precision = 10, scale = 2)
    private BigDecimal totalDepositAmount = BigDecimal.ZERO;


    @Column(columnDefinition = "TEXT")
    private String notes;


    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseInvoiceItem> items = new ArrayList<>();


    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public boolean isDraft() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean isApproved() {
        return status == InvoiceStatus.APPROVED;
    }

    public boolean isCancelled() {
        return status == InvoiceStatus.CANCELLED;
    }

    public void approve(User approver) {
        if (!isDraft()) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be approved. Current status: " + status);
        }
        this.status = InvoiceStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void cancel() {
        if (!isDraft()) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be cancelled. Current status: " + status);
        }
        this.status = InvoiceStatus.CANCELLED;
    }
}
