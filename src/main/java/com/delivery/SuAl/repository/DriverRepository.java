package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.model.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);

    @Query("SELECT d FROM Driver d JOIN d.user u " +
            "WHERE u.phoneNumber =:phoneNumber")
    Optional<Driver> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT d FROM Driver d JOIN d.user u WHERE u.email = :email")
    Optional<Driver> findByEmail(@Param("email") String email);

    List<Driver> findByDriverStatus(DriverStatus driverStatus);

    @Query("SELECT d FROM Driver d " +
            "WHERE d.driverStatus = 'ACTIVE' " +
            "ORDER BY d.firstName")
    List<Driver> findAllActive();

    @Query("SELECT d FROM Driver d JOIN d.user u " +
            "WHERE LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR u.phoneNumber LIKE CONCAT('%', :search, '%') " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Driver> searchDrivers(@Param("search") String search);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Driver d JOIN d.user u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Driver d JOIN d.user u WHERE u.phoneNumber = :phoneNumber")
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
