package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByPhoneNumberAndIsUsedFalse(String phoneNumber);

    void deleteByPhoneNumber(String phoneNumber);

    @Modifying
    @Query("DELETE FROM OtpCode o " +
            "WHERE o.expiresAt < :now")
    void deleteExpiredOtps(LocalDateTime now);
}
