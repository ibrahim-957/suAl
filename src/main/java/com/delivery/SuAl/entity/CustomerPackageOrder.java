package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.enums.PackageOrderStatus;
import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_package_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPackageOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private AffordablePackage affordablePackage;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private Integer frequency;

    @Column(name = "package_product_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal packageProductPrice;

    @Column(name = "total_containers_in_package", nullable = false)
    private Integer totalContainersInPackage;

    @Column(name = "old_containers_to_collect")
    private Integer oldContainersToCollect = 0; // Old containers from previous orders

    @Column(name = "total_deposit_charged", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDepositCharged = BigDecimal.ZERO;

    @Column(name = "expected_deposit_refunded", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedDepositRefunded = BigDecimal.ZERO;

    @Column(name = "actual_deposit_refunded", precision = 10, scale = 2)
    private BigDecimal actualDepositRefunded = BigDecimal.ZERO;

    @Column(name = "net_deposit", nullable = false, precision = 10, scale = 2)
    private BigDecimal netDeposit = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice; // packageProductPrice + netDeposit

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "amount_collected_at_delivery_1", precision = 10, scale = 2)
    private BigDecimal amountCollectedAtDelivery1; // For COD packages


    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private PackageOrderStatus orderStatus = PackageOrderStatus.PENDING;

    @Column(name = "order_month", nullable = false, length = 7)
    private String orderMonth;

    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    @OneToMany(mappedBy = "packageOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PackageDeliveryDistribution> deliveryDistributions = new ArrayList<>();

    @OneToMany(mappedBy = "packageOrder")
    private List<Order> generatedOrders = new ArrayList<>();

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
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


    public boolean isCancelled() {
        return orderStatus == PackageOrderStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return orderStatus == PackageOrderStatus.COMPLETED;
    }

    public boolean isPending() {
        return orderStatus == PackageOrderStatus.PENDING;
    }

    public boolean isActive() {
        return orderStatus == PackageOrderStatus.ACTIVE;
    }
}
