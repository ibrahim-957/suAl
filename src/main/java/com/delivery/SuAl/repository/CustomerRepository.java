package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT c FROM Customer c JOIN c.user u WHERE u.phoneNumber = :phoneNumber")
    Optional<Customer> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT c FROM Customer c LEFT JOIN c.user u WHERE u.phoneNumber = :phoneNumber OR (c.isActive = false AND c.user IS NULL)")
    Optional<Customer> findByPhoneNumberIncludingInactive(@Param("phoneNumber") String phoneNumber);

}
