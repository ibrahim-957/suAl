package com.delivery.SuAl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "container_collections")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContainerCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "empty_containers", nullable = false)
    private Integer emptyContainers = 0;

    @Column(name = "damaged_containers", nullable = false)
    private Integer damagedContainers = 0;

    @Column(name = "total_collected", nullable = false)
    private Integer totalCollected = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_user_id")
    private User collectedBy;

    @Column(name = "collection_date_time", nullable = false)
    private LocalDateTime collectionDateTime;

    private String notes;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime  createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (collectionDateTime == null) {
            collectionDateTime = LocalDateTime.now();
        }
        totalCollected = (emptyContainers != null ? emptyContainers : 0) +
                (damagedContainers != null ? damagedContainers : 0);
    }
}
