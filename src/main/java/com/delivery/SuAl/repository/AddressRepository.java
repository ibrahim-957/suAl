package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCity(String city);

    @Query("SELECT a FROM Address a " +
            "WHERE LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.city) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Address> searchAddresses(@Param("search") String search);
}
