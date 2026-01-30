package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    Optional<User> findByTargetIdAndRole(Long targetId, UserRole role);

    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phone OR u.email = :email")
    Optional<User> findByPhoneNumberOrEmail(@Param("phone") String phoneNumber,
                                            @Param("email") String email);

    void deleteByTargetIdAndRole(Long targetId, UserRole role);
}
