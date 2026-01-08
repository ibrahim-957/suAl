package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.model.enums.OperatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    List<Operator> findByOperatorStatus (OperatorStatus operatorStatus);

    Optional<Operator> findByPhoneNumber (String phoneNumber);

    Optional<Operator> findByEmail (String email);

    @Query("SELECT o FROM Operator o WHERE o.operatorStatus = 'ACTIVE' ORDER BY o.firstName")
    List<Operator> findAllActive();
}
