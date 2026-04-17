package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.CampaignUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CampaignUsageRepository extends JpaRepository<CampaignUsage, Long> {
    int countByCustomerIdAndCampaignCampaignCode(Long customerId, String campaignCode);

    @Modifying
    @Query("DELETE FROM CampaignUsage cu WHERE cu.order.id = :orderId")
    int deleteByOrderId(Long orderId);

    List<CampaignUsage> findByOrderId(Long orderId);
}
