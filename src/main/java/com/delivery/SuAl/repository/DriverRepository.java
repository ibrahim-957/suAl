package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByDriverStatus(DriverStatus driverStatus);

    Optional<Driver> findByPhoneNumber(String phoneNumber);

    @Query("SELECT d FROM Driver d " +
            "WHERE d.driverStatus = 'ACTIVE' " +
            "ORDER BY d.firstName")
    List<Driver> findAllActive();

    @Query("SELECT d FROM Driver d " +
            "WHERE LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR d.phoneNumber LIKE CONCAT('%', :search, '%')")
    List<Driver> searchDrivers(@Param("search") String search);
}
