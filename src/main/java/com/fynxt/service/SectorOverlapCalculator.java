package com.fynxt.service;

import com.fynxt.dto.SecterOverlapResponse;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SectorOverlapCalculator {

    private static final Map<String, Set<String>> BASKETS =
            new LinkedHashMap<>();

    static {
        BASKETS.put(
                "TECH_HEAVY",
                Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA")
        );

        BASKETS.put(
                "FINANCE_HEAVY",
                Set.of("JPM", "GS", "BAC", "MS", "WFC")
        );

        BASKETS.put(
                "BALANCED",
                Set.of("AAPL", "JPM", "XOM", "JNJ", "TSLA")
        );
    }

    public SecterOverlapResponse calculate(Set<String> portfolioStocks) {

        Map<String, Double> overlapPercentages =
                new LinkedHashMap<>();

        String dominantBasket = null;
        double highestOverlap = -1.0;

        for (Map.Entry<String, Set<String>> entry : BASKETS.entrySet()) {

            String basketName = entry.getKey();
            Set<String> basketStocks = entry.getValue();

            long commonStocks = portfolioStocks.stream()
                    .filter(basketStocks::contains)
                    .count();

            double overlap = calculateOverlap(
                    portfolioStocks.size(),
                    basketStocks.size(),
                    commonStocks
            );

            overlapPercentages.put(
                    basketName,
                    overlap
            );

            if (overlap > highestOverlap) {
                highestOverlap = overlap;
                dominantBasket = basketName;
            }
        }

        String riskFlag = calculateRiskFlag(overlapPercentages);

        return new SecterOverlapResponse(
                overlapPercentages,
                dominantBasket,
                riskFlag
        );
    }

    private double calculateOverlap(
            int portfolioSize,
            int basketSize,
            long commonStocks) {

        if (portfolioSize + basketSize == 0) {
            return 0.0;
        }

        return (2.0 * commonStocks
                / (portfolioSize + basketSize)) * 100;
    }

    private String calculateRiskFlag(
            Map<String, Double> overlapPercentages) {

        boolean highRisk = overlapPercentages.values()
                .stream()
                .anyMatch(value -> value >= 60.0);

        if (highRisk) {
            return "HIGH";
        }

        boolean mediumRisk = overlapPercentages.values()
                .stream()
                .anyMatch(value -> value >= 40.0);

        if (mediumRisk) {
            return "MEDIUM";
        }

        return "LOW";
    }
}