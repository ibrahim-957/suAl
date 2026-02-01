package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.model.enums.CampaignStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCampaignCode(String campaignCode);

    boolean existsByCampaignCode(String campaignCode);

    List<Campaign> findByCampaignStatus(CampaignStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campaign c WHERE c.campaignCode = :campaignCode")
    Optional<Campaign> findByCampaignCodeWithLock(@Param("campaignCode") String campaignCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campaign c WHERE c.id = :id")
    Optional<Campaign> findByIdWithLock(@Param("id") Long id);
}
