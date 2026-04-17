package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT a FROM Admin a JOIN a.user u " +
            "WHERE u.email =:email")
    Optional<Admin> findByEmail (@Param("email") String email);

    @Query("SELECT a FROM Admin a " +
            "WHERE a.user.id IN :userIds")
    List<Admin> findByUserIdIn(@Param("userIds") List<Long> userIds);
}
