package com.fynxt.service;

import com.fynxt.dto.SecterOverlapResponse;
import com.fynxt.entity.PortfolioHolding;
import com.fynxt.entity.Trader;
import com.fynxt.exception.ResourceNotFoundException;
import com.fynxt.repository.PortfolioHoldingRepository;
import com.fynxt.repository.TraderRepository;
import com.fynxt.service.impl.SectorOverlapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorOverlapServiceImplTest {

    @Mock
    private TraderRepository traderRepository;

    @Mock
    private PortfolioHoldingRepository portfolioHoldingRepository;

    @InjectMocks
    private SectorOverlapServiceImpl sectorOverlapService;

    private Trader trader;

    @BeforeEach
    void setUp() {
        trader = new Trader(
                "T001",
                "Demo Trader",
                LocalDateTime.now()
        );
    }

    @Test
    void shouldCalculateSectorOverlapSuccessfully() {

        PortfolioHolding apple = createHolding(
                "T001", "AAPL", "TECH", 100
        );

        PortfolioHolding microsoft = createHolding(
                "T001", "MSFT", "TECH", 50
        );

        PortfolioHolding jpm = createHolding(
                "T001", "JPM", "FINANCE", 100
        );

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of(apple, microsoft, jpm));

        SecterOverlapResponse response =
                sectorOverlapService.getSectorOverlap("T001");

        assertNotNull(response);

        assertEquals(
                50.0,
                response.getOverlapPercentages()
                        .get("TECH_HEAVY")
        );

        assertEquals(
                25.0,
                response.getOverlapPercentages()
                        .get("FINANCE_HEAVY")
        );

        assertEquals(
                50.0,
                response.getOverlapPercentages()
                        .get("BALANCED")
        );

        assertEquals(
                "TECH_HEAVY",
                response.getDominantBasket()
        );

        assertEquals(
                "MEDIUM",
                response.getRiskFlag()
        );
    }

    @Test
    void shouldIgnoreZeroQuantityHoldings() {

        PortfolioHolding apple = createHolding(
                "T001", "AAPL", "TECH", 100
        );

        PortfolioHolding microsoft = createHolding(
                "T001", "MSFT", "TECH", 0
        );

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of(apple, microsoft));

        SecterOverlapResponse response =
                sectorOverlapService.getSectorOverlap("T001");

        assertEquals(
                33.33333333333333,
                response.getOverlapPercentages()
                        .get("TECH_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("FINANCE_HEAVY")
        );

        assertEquals(
                33.33333333333333,
                response.getOverlapPercentages()
                        .get("BALANCED")
        );

        assertEquals(
                "LOW",
                response.getRiskFlag()
        );
    }

    @Test
    void shouldReturnHighRiskForTechHeavyPortfolio() {

        List<PortfolioHolding> holdings = List.of(
                createHolding("T001", "AAPL", "TECH", 100),
                createHolding("T001", "MSFT", "TECH", 100),
                createHolding("T001", "GOOGL", "TECH", 100),
                createHolding("T001", "TSLA", "TECH", 100),
                createHolding("T001", "NVDA", "TECH", 100)
        );

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(holdings);

        SecterOverlapResponse response =
                sectorOverlapService.getSectorOverlap("T001");

        assertEquals(
                "TECH_HEAVY",
                response.getDominantBasket()
        );

        assertEquals(
                "HIGH",
                response.getRiskFlag()
        );

        assertEquals(
                100.0,
                response.getOverlapPercentages()
                        .get("TECH_HEAVY")
        );
    }

    @Test
    void shouldReturnLowRiskForUnrelatedPortfolio() {

        List<PortfolioHolding> holdings = List.of(
                createHolding("T001", "IBM", "TECH", 100),
                createHolding("T001", "ORCL", "TECH", 100)
        );

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(holdings);

        SecterOverlapResponse response =
                sectorOverlapService.getSectorOverlap("T001");

        assertEquals(
                "LOW",
                response.getRiskFlag()
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("TECH_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("FINANCE_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("BALANCED")
        );
    }

    @Test
    void shouldReturnEmptyOverlapForEmptyPortfolio() {

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of());

        SecterOverlapResponse response =
                sectorOverlapService.getSectorOverlap("T001");

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("TECH_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("FINANCE_HEAVY")
        );

        assertEquals(
                0.0,
                response.getOverlapPercentages()
                        .get("BALANCED")
        );

        assertEquals(
                "LOW",
                response.getRiskFlag()
        );
    }

    @Test
    void shouldThrowExceptionWhenTraderNotFound() {

        when(traderRepository.findById("T999"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> sectorOverlapService
                                .getSectorOverlap("T999")
                );

        assertEquals(
                "Trader not found: T999",
                exception.getMessage()
        );

        verify(
                portfolioHoldingRepository,
                never()
        ).findAllByTraderId("T999");
    }

    private PortfolioHolding createHolding(
            String traderId,
            String stock,
            String sector,
            Integer quantity) {

        LocalDateTime now = LocalDateTime.now();

        PortfolioHolding holding =
                new PortfolioHolding();

        holding.setTraderId(traderId);
        holding.setStock(stock);
        holding.setSector(sector);
        holding.setQuantity(quantity);
        holding.setReservedQuantity(0);
        holding.setCreatedAt(now);
        holding.setUpdatedAt(now);

        return holding;
    }
}
