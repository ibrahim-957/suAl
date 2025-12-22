package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    Optional<User> findByIdAndIsActiveTrue(Long id);

    Boolean existsByPhoneNumber(String phoneNumber);
}
