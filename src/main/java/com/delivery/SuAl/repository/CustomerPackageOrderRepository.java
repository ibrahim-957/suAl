package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.model.enums.PackageOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPackageOrderRepository extends JpaRepository<CustomerPackageOrder, Long> {
    @Query("SELECT cpo FROM CustomerPackageOrder cpo WHERE cpo.customer.id = :customerId")
    Page<CustomerPackageOrder> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT cpo FROM CustomerPackageOrder cpo " +
            "WHERE cpo.customer.id = :customerId " +
            "AND cpo.orderMonth = :orderMonth " +
            "AND cpo.orderStatus IN :statuses")
    List<CustomerPackageOrder> findByCustomerIdAndMonthAndStatuses(
            @Param("customerId") Long customerId,
            @Param("orderMonth") String orderMonth,
            @Param("statuses") List<PackageOrderStatus> statuses
    );

    @Query("SELECT cpo FROM CustomerPackageOrder cpo " +
            "WHERE cpo.customer.id = :customerId " +
            "AND cpo.orderMonth = :orderMonth " +
            "AND cpo.orderStatus NOT IN ('CANCELLED', 'COMPLETED')")
    Optional<CustomerPackageOrder> findActivePackageForCustomerInMonth(
            @Param("customerId") Long customerId,
            @Param("orderMonth") String orderMonth
    );

    @Query("SELECT cpo FROM CustomerPackageOrder cpo " +
            "WHERE cpo.orderStatus = :status")
    Page<CustomerPackageOrder> findByStatus(@Param("status") PackageOrderStatus status, Pageable pageable);

    @Query("SELECT cpo FROM CustomerPackageOrder cpo " +
            "WHERE cpo.orderMonth = :orderMonth " +
            "AND cpo.autoRenew = true " +
            "AND cpo.orderStatus = 'COMPLETED'")
    List<CustomerPackageOrder> findCompletedPackagesForAutoRenewal(@Param("orderMonth") String orderMonth);


    @Query("SELECT COUNT(cpo) FROM CustomerPackageOrder cpo " +
            "WHERE cpo.customer.id = :customerId " +
            "AND cpo.orderMonth = :orderMonth " +
            "AND cpo.orderStatus NOT IN ('CANCELLED')")
    long countActivePackagesForCustomerInMonth(
            @Param("customerId") Long customerId,
            @Param("orderMonth") String orderMonth
    );

    Optional<CustomerPackageOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT cpo FROM CustomerPackageOrder cpo " +
            "JOIN FETCH cpo.customer " +
            "JOIN FETCH cpo.affordablePackage " +
            "LEFT JOIN FETCH cpo.deliveryDistributions " +
            "WHERE cpo.id = :id")
    Optional<CustomerPackageOrder> findByIdWithDetails(@Param("id") Long id);
}
