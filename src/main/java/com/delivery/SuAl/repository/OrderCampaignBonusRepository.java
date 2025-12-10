package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.OrderCampaignBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderCampaignBonusRepository extends JpaRepository<OrderCampaignBonus, Long> {
    List<OrderCampaignBonus> findByOrderId(Long orderId);

    List<OrderCampaignBonus> findByCampaignId(Long campaignId);

    @Query("SELECT COUNT(ocb) FROM OrderCampaignBonus ocb " +
            "WHERE ocb.campaign.id = :campaignId")
    Long countByCampaignId(@Param("campaignId") Long campaignId);
}
