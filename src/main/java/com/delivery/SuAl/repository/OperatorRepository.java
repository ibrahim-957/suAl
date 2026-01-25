package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.model.enums.OperatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    Optional<Operator> findByUserId(Long userId);

    @Query("SELECT o FROM Operator o JOIN o.user u " +
            "WHERE u.email =:email")
    Optional<Operator> findByEmail (@Param("email") String email);

    @Query("SELECT o FROM Operator o JOIN o.user u " +
            "WHERE u.phoneNumber =:phoneNumber")
    Optional<Operator> findByPhoneNumber (@Param("phoneNumber") String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Operator o JOIN o.user u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);
}
