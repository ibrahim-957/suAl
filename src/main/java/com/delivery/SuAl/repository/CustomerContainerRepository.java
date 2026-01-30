package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.CustomerContainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerContainerRepository extends JpaRepository<CustomerContainer,Long> {
    Optional<CustomerContainer> findByCustomerIdAndProductId(Long customerId,  Long productId);
}
