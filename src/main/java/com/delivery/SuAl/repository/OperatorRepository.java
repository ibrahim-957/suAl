package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.model.enums.OperatorStatus;
import com.delivery.SuAl.model.enums.OperatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    @Query("SELECT o FROM Operator o JOIN o.user u " +
            "WHERE u.email =:email")
    Optional<Operator> findByEmail (@Param("email") String email);

    @Query("SELECT o FROM Operator o JOIN o.user u " +
            "WHERE u.phoneNumber =:phoneNumber")
    Optional<Operator> findByPhoneNumber (@Param("phoneNumber") String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Operator o JOIN o.user u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    List<Operator> findByOperatorStatus(OperatorStatus operatorStatus);

    @Query("SELECT o FROM Operator o " +
            "LEFT JOIN FETCH o.company " +
            "WHERE o.user.email = :email")
    Optional<Operator> findByUserEmail(@Param("email") String email);

    @Query("SELECT o FROM Operator o " +
            "WHERE o.operatorStatus = :status " +
            "AND o.company.id = :companyId")
    List<Operator> findByOperatorStatusAndCompanyId(
            @Param("status") OperatorStatus status,
            @Param("companyId") Long companyId);

    @Query("SELECT o FROM Operator o " +
            "WHERE o.operatorStatus = :status " +
            "AND o.operatorType = :operatorType")
    List<Operator> findByOperatorStatusAndOperatorType(
            @Param("status") OperatorStatus status,
            @Param("operatorType") OperatorType operatorType
    );

    @Query("SELECT DISTINCT o FROM Operator o " +
            "WHERE o.operatorStatus = :status " +
            "AND (o.operatorType = 'SYSTEM' OR o.company.id IN :companyIds)")
    List<Operator> findOperatorsToNotify(
            @Param("status") OperatorStatus status,
            @Param("companyIds") List<Long> companyIds
    );
}
