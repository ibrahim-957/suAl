package com.delivery.SuAl.entity;
import com.delivery.SuAl.model.enums.TransferStatus;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse_transfers", indexes = {
        @Index(name = "idx_transfer_from_warehouse", columnList = "from_warehouse_id"),
        @Index(name = "idx_transfer_to_warehouse",   columnList = "to_warehouse_id"),
        @Index(name = "idx_transfer_status",         columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, updatable = false)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id", nullable = false)
    private Warehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    private Warehouse toWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;


    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @Column(columnDefinition = "TEXT")
    private String notes;


    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseTransferItem> items = new ArrayList<>();


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


    public boolean isPending() {
        return status == TransferStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == TransferStatus.CANCELLED;
    }

    public void complete(User completedBy) {
        if (!isPending()) {
            throw new IllegalStateException(
                    "Only PENDING transfers can be completed. Current status: " + status);
        }
        if (fromWarehouse.getId().equals(toWarehouse.getId())) {
            throw new IllegalStateException(
                    "Source and destination warehouse cannot be the same");
        }
        this.status = TransferStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void cancel() {
        if (!isPending()) {
            throw new IllegalStateException(
                    "Only PENDING transfers can be cancelled. Current status: " + status);
        }
        this.status = TransferStatus.CANCELLED;
    }
}

