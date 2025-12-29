package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.CampaignUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignUsageRepository extends JpaRepository<CampaignUsage, Long> {
    List<CampaignUsage> findByUserId(Long userId);

    Page<CampaignUsage> findByUserId(Long userId, Pageable pageable);

    List<CampaignUsage> findByCampaignId(Long campaignId);

    Page<CampaignUsage> findByCampaignId(Long campaignId, Pageable pageable);

    Long countByUserIdAndCampaignId(Long userId, Long campaignId);

    @Query("SELECT CASE WHEN COUNT(cu) > 0 THEN TRUE ELSE FALSE END FROM CampaignUsage cu " +
            "WHERE cu.user.id = :userId AND cu.campaign.id = :campaignId")
    Boolean hasUserUsedCampaign(@Param("userId") Long userId, @Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(cu) FROM CampaignUsage cu " +
            "WHERE cu.user.id = :userId AND cu.campaign.id = :campaignId")
    Integer countUsagesByUserAndCampaign(@Param("userId") Long userId, @Param("campaignId") Long campaignId);
}
