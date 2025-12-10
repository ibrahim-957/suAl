package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car,Long> {
    List<Car> findByDriverId(Long driverId);

    Optional<Car> findByPlateNumber(String plateNumber);
}
