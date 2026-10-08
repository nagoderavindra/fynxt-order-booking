package com.fynxt.dto;

import java.util.Map;

public class SecterOverlapResponse {

    private Map<String, Double> overlapPercentages;

    private String dominantBasket;

    private String riskFlag;

    public SecterOverlapResponse() {
    }

    public SecterOverlapResponse(
            Map<String, Double> overlapPercentages,
            String dominantBasket,
            String riskFlag) {

        this.overlapPercentages = overlapPercentages;
        this.dominantBasket = dominantBasket;
        this.riskFlag = riskFlag;
    }

    public Map<String, Double> getOverlapPercentages() {
        return overlapPercentages;
    }

    public void setOverlapPercentages(
            Map<String, Double> overlapPercentages) {
        this.overlapPercentages = overlapPercentages;
    }

    public String getDominantBasket() {
        return dominantBasket;
    }

    public void setDominantBasket(String dominantBasket) {
        this.dominantBasket = dominantBasket;
    }

    public String getRiskFlag() {
        return riskFlag;
    }

    public void setRiskFlag(String riskFlag) {
        this.riskFlag = riskFlag;
    }
}