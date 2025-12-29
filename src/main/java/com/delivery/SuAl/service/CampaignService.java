package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import com.delivery.SuAl.model.response.marketing.ValidateCampaignResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignService {
    CampaignResponse createCampaign(CreateCampaignRequest request);
    CampaignResponse getCampaignById(Long id);
    Page<CampaignResponse> getCampaigns(Pageable pageable);
    CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request);
    void deleteCampaignById(Long id);
    ValidateCampaignResponse validateCampaign(ValidateCampaignRequest request);
    ApplyCampaignResponse applyCampaign(ApplyCampaignRequest request);
}
