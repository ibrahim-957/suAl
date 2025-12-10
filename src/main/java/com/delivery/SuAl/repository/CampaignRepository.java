package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.model.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCampaignId(String campaignId);

    List<Campaign> findByCampaignStatus(CampaignStatus campaignStatus);

    @Query("SELECT c FROM Campaign c " +
            "WHERE c.campaignStatus = 'ACTIVE' " +
            "AND (c.validFrom IS NULL OR c.validFrom <= CURRENT_DATE) " +
            "AND (c.validTo IS NULL OR c.validTo >= CURRENT_DATE)")
    List<Campaign> findActiveCampaigns();

    List<Campaign> findByBuyProductId(Long buyProductId);

    List<Campaign> findByFreeProductId(Long freeProductId);

    @Query("SELECT c FROM Campaign c " +
            "WHERE c.campaignStatus = 'ACTIVE' AND c.validTo BETWEEN CURRENT_DATE AND :endDate " +
            "ORDER BY c.validTo ASC")
    List<Campaign> findExpiringSoon(@Param("endDate") LocalDate endDate);
}
