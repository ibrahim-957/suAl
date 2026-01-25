package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByUserIdAndIsActiveTrue(Long userId);

    Optional<Customer> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT c FROM Customer c JOIN c.user u WHERE u.phoneNumber = :phoneNumber")
    Optional<Customer> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT c FROM Customer c JOIN c.user u WHERE u.phoneNumber = :phoneNumber AND c.isActive = true")
    Optional<Customer> findByPhoneNumberAndIsActiveTrue(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c JOIN c.user u WHERE u.phoneNumber = :phoneNumber")
    Boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT c FROM Customer c JOIN c.user u " +
            "WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR u.phoneNumber LIKE CONCAT('%', :search, '%')")
    List<Customer> searchCustomers(@Param("search") String search);

    @Query("SELECT c FROM Customer c WHERE c.isActive = true ORDER BY c.firstName")
    List<Customer> findAllActive();
}
