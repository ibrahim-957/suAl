package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u " +
            "WHERE u.role =:role AND u.targetId =:targetId")
    Optional<User> findByRoleAndTargetId(@Param("role") UserRole role, @Param("targetId") Long targetId);

    List<User> findByRole(UserRole role);
}
