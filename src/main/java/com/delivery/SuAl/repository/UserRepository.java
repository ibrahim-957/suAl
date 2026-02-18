package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndRole(String email, UserRole role);

    Optional<User> findByPhoneNumberAndRole(String phoneNumber, UserRole role);

    boolean existsByEmailAndRole(String email, UserRole role);

    boolean existsByPhoneNumberAndRole(String phoneNumber, UserRole role);

    void deleteByTargetIdAndRole(Long targetId, UserRole role);

    Optional<User> findFirstByEmailAndRoleNot(String email, UserRole excludedRole);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumberOrEmail(String phoneNumber, String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}