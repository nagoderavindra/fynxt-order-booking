package com.fynxt.service;

import com.fynxt.dto.SecterOverlapResponse;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SectorOverlapCalculatorTest {

    private final SectorOverlapCalculator calculator =
            new SectorOverlapCalculator();

    @Test
    void shouldCalculateOverlapCorrectly() {

        Set<String> portfolioStocks =
                Set.of("AAPL", "MSFT", "GOOGL", "JPM", "XOM");

        SecterOverlapResponse response =
                calculator.calculate(portfolioStocks);

        assertEquals(
                60.0,
                response.getOverlapPercentages().get("TECH_HEAVY")
        );

        assertEquals(
                20.0,
                response.getOverlapPercentages().get("FINANCE_HEAVY")
        );

        assertEquals(
                60.0,
                response.getOverlapPercentages().get("BALANCED")
        );
    }

    @Test
    void shouldReturnHighRiskWhenOverlapIs60OrMore() {

        Set<String> portfolioStocks =
                Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA");

        SecterOverlapResponse response =
                calculator.calculate(portfolioStocks);

        assertEquals("HIGH", response.getRiskFlag());
    }

    @Test
    void shouldReturnMediumRiskWhenOverlapIs40OrMoreButLessThan60() {

        Set<String> portfolioStocks =
                Set.of("AAPL", "JPM", "ABC", "XYZ", "PQR");

        SecterOverlapResponse response =
                calculator.calculate(portfolioStocks);

        assertEquals("MEDIUM", response.getRiskFlag());
    }

    @Test
    void shouldReturnLowRiskWhenAllOverlapsAreBelow40() {

        Set<String> portfolioStocks =
                Set.of("IBM", "ORCL");

        SecterOverlapResponse response =
                calculator.calculate(portfolioStocks);

        assertEquals("LOW", response.getRiskFlag());
    }

    @Test
    void shouldIdentifyDominantBasket() {

        Set<String> portfolioStocks =
                Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA");

        SecterOverlapResponse response =
                calculator.calculate(portfolioStocks);

        assertEquals(
                "TECH_HEAVY",
                response.getDominantBasket()
        );
    }

    @Test
    void shouldReturnZeroOverlapForEmptyPortfolio() {

        Set<String> portfolioStocks = Set.of();

        SecterOverlapResponse response =
                calculator.calculate(portfolioStocks);

        assertEquals(
                0.0,
                response.getOverlapPercentages().get("TECH_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages().get("FINANCE_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages().get("BALANCED")
        );

        assertEquals("LOW", response.getRiskFlag());
    }
}