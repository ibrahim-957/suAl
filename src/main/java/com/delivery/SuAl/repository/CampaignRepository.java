package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.model.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCampaignCode(String campaignCode);

    boolean existsByCampaignCode(String campaignCode);

    List<Campaign> findByCampaignStatus(CampaignStatus status);

}
