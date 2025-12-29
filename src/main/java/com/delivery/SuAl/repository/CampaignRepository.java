package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.model.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCampaignId(String  campaignId);

    List<Campaign> findByCampaignStatus(CampaignStatus campaignStatus);

    Page<Campaign> findByCampaignStatus(CampaignStatus campaignStatus, Pageable pageable);

    @Query("SELECT c FROM Campaign c " +
            "WHERE c.campaignId = :campaignId " +
            "AND c.campaignStatus = 'ACTIVE' " +
            "AND :now BETWEEN c.validFrom AND c.validTo")
    Optional<Campaign> findActiveByCampaignId(@Param("campaignId") Long campaignId,
                                              @Param("now") LocalDate now);

    @Query("SELECT c FROM Campaign c " +
            "WHERE c.campaignStatus = 'ACTIVE' " +
            "AND :now BETWEEN c.validFrom AND c.validTo")
    List<Campaign> findActiveCampaigns(@Param("now") LocalDate now);

    @Query("SELECT c FROM Campaign c WHERE c.validTo < :now")
    List<Campaign> findExpiredCampaigns(@Param("now") LocalDate now);

    @Modifying
    @Query("UPDATE Campaign c SET c.currentTotalUses = c.currentTotalUses + 1 WHERE c.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    @Query("SELECT CASE WHEN (c.maxTotalUses IS NULL OR c.currentTotalUses < c.maxTotalUses) " +
            "THEN true ELSE false END FROM Campaign c WHERE c.id = :id")
    Boolean hasUsageAvailable(@Param("id") Long id);

    Boolean existsByCampaignId(String campaignId);
}
