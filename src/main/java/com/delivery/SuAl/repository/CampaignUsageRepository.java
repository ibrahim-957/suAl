package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.CampaignUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignUsageRepository extends JpaRepository<CampaignUsage, Long> {
    int countByUserIdAndCampaignCampaignCode(Long userId, String campaignCode);

    List<CampaignUsage> findByUserId(Long userId);

    List<CampaignUsage> findByCampaignCampaignCode(String campaignCode);

    Optional<CampaignUsage> findByOrderId(Long orderId);
}
