package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.OrderCampaignBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderCampaignBonusRepository extends JpaRepository<OrderCampaignBonus, Long> {
    List<OrderCampaignBonus> findByOrderId(Long orderId);

    List<OrderCampaignBonus> findByCampaignId(Long campaignId);

    @Query("SELECT SUM(ocb.originalValue) FROM OrderCampaignBonus ocb " +
            "WHERE ocb.campaign.id = :campaignId")
    BigDecimal sumOriginalValueByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT SUM(ocb.quantity) FROM OrderCampaignBonus ocb " +
            "WHERE ocb.campaign.id = :campaignId")
    Integer sumQuantityByCampaignId(@Param("campaignId") Long campaignId);
}
