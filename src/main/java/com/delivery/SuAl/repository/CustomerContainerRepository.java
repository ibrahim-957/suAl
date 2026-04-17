package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.CustomerContainer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContainerRepository extends JpaRepository<CustomerContainer, Long> {

    @Query("SELECT cc FROM CustomerContainer cc WHERE cc.customer.id = :customerId")
    List<CustomerContainer> findByCustomerId(@Param("customerId") Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cc FROM CustomerContainer cc WHERE cc.customer.id = :customerId AND cc.product.id = :productId")
    Optional<CustomerContainer> findByCustomerIdAndProductIdWithLock(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );

    @Query("SELECT cc FROM CustomerContainer cc WHERE cc.customer.id = :customerId AND cc.product.id = :productId")
    Optional<CustomerContainer> findByCustomerIdAndProductId(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );

}