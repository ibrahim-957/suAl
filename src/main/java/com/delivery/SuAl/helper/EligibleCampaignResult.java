package com.delivery.SuAl.helper;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EligibleCampaignResult {
    private boolean skip;
    private EligibleCampaignInfo campaignInfo;

    public static EligibleCampaignResult skip(){
        return  new EligibleCampaignResult(true, null);
    }

    public static EligibleCampaignResult withInfo(EligibleCampaignInfo info){
        return new EligibleCampaignResult(false, info);
    }

    public boolean shouldSkip(){
        return skip;
    }
}
