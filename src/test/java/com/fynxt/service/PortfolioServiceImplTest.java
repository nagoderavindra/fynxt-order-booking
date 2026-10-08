package com.fynxt.service;

import com.fynxt.dto.AddToPortfolioRequest;
import com.fynxt.dto.PortfolioResponse;
import com.fynxt.entity.PortfolioHolding;
import com.fynxt.entity.Trader;
import com.fynxt.exception.BusinessException;
import com.fynxt.exception.ResourceNotFoundException;
import com.fynxt.repository.PortfolioHoldingRepository;
import com.fynxt.repository.TraderRepository;
import com.fynxt.service.impl.PortfolioServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private PortfolioHoldingRepository portfolioHoldingRepository;

    @Mock
    private TraderRepository traderRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

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
    void shouldReturnPortfolioSuccessfully() {

        PortfolioHolding apple = createHolding(
                "T001",
                "AAPL",
                "TECH",
                100,
                0
        );

        PortfolioHolding jpm = createHolding(
                "T001",
                "JPM",
                "FINANCE",
                50,
                0
        );

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of(apple, jpm));

        PortfolioResponse response =
                portfolioService.getPortfolio("T001");

        assertNotNull(response);

        assertEquals("T001", response.getTraderId());

        assertEquals(100,
                response.getPositions().get("AAPL"));

        assertEquals(50,
                response.getPositions().get("JPM"));

        assertEquals(100,
                response.getSectorBreakdown().get("TECH"));

        assertEquals(50,
                response.getSectorBreakdown().get("FINANCE"));
    }

    @Test
    void shouldIgnoreZeroQuantityHolding() {

        PortfolioHolding apple = createHolding(
                "T001",
                "AAPL",
                "TECH",
                100,
                0
        );

        PortfolioHolding microsoft = createHolding(
                "T001",
                "MSFT",
                "TECH",
                0,
                0
        );

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of(apple, microsoft));

        PortfolioResponse response =
                portfolioService.getPortfolio("T001");

        assertEquals(1, response.getPositions().size());

        assertTrue(response.getPositions().containsKey("AAPL"));

        assertFalse(response.getPositions().containsKey("MSFT"));
    }

    @Test
    void shouldReturnEmptyPortfolioWhenTraderHasNoHoldings() {

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of());

        PortfolioResponse response =
                portfolioService.getPortfolio("T001");

        assertNotNull(response);

        assertTrue(response.getPositions().isEmpty());

        assertTrue(response.getSectorBreakdown().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenTraderNotFound() {

        when(traderRepository.findById("T999"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> portfolioService.getPortfolio("T999")
                );

        assertEquals(
                "Trader not found: T999",
                exception.getMessage()
        );

        verify(portfolioHoldingRepository, never())
                .findAllByTraderId("T999");
    }

    @Test
    void shouldAddNewHoldingSuccessfully() {

        AddToPortfolioRequest request =
                new AddToPortfolioRequest();

        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(100);

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository
                .findByTraderIdAndStockForUpdate(
                        "T001",
                        "AAPL"))
                .thenReturn(Optional.empty());

        when(portfolioHoldingRepository.save(
                any(PortfolioHolding.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of(
                        createHolding(
                                "T001",
                                "AAPL",
                                "TECH",
                                100,
                                0
                        )
                ));

        PortfolioResponse response =
                portfolioService.addToPortfolio(
                        "T001",
                        request
                );

        assertNotNull(response);

        assertEquals(
                100,
                response.getPositions().get("AAPL")
        );

        verify(portfolioHoldingRepository)
                .save(any(PortfolioHolding.class));
    }

    @Test
    void shouldIncreaseExistingHoldingQuantity() {

        AddToPortfolioRequest request =
                new AddToPortfolioRequest();

        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(50);

        PortfolioHolding existingHolding =
                createHolding(
                        "T001",
                        "AAPL",
                        "TECH",
                        100,
                        0
                );

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository
                .findByTraderIdAndStockForUpdate(
                        "T001",
                        "AAPL"))
                .thenReturn(Optional.of(existingHolding));

        when(portfolioHoldingRepository.save(
                any(PortfolioHolding.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(traderRepository.findById("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository.findAllByTraderId("T001"))
                .thenReturn(List.of(existingHolding));

        PortfolioResponse response =
                portfolioService.addToPortfolio(
                        "T001",
                        request
                );

        assertEquals(
                150,
                existingHolding.getQuantity()
        );

        assertEquals(
                150,
                response.getPositions().get("AAPL")
        );
    }

    @Test
    void shouldRejectSectorMismatch() {

        AddToPortfolioRequest request =
                new AddToPortfolioRequest();

        request.setStock("AAPL");
        request.setSector("FINANCE");
        request.setQuantity(50);

        PortfolioHolding existingHolding =
                createHolding(
                        "T001",
                        "AAPL",
                        "TECH",
                        100,
                        0
                );

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(portfolioHoldingRepository
                .findByTraderIdAndStockForUpdate(
                        "T001",
                        "AAPL"))
                .thenReturn(Optional.of(existingHolding));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> portfolioService.addToPortfolio(
                                "T001",
                                request
                        )
                );

        assertEquals(
                "Sector mismatch for stock: AAPL",
                exception.getMessage()
        );

        verify(portfolioHoldingRepository, never())
                .save(any(PortfolioHolding.class));
    }

    private PortfolioHolding createHolding(
            String traderId,
            String stock,
            String sector,
            Integer quantity,
            Integer reservedQuantity) {

        LocalDateTime now = LocalDateTime.now();

        PortfolioHolding holding =
                new PortfolioHolding();

        holding.setTraderId(traderId);
        holding.setStock(stock);
        holding.setSector(sector);
        holding.setQuantity(quantity);
        holding.setReservedQuantity(reservedQuantity);
        holding.setCreatedAt(now);
        holding.setUpdatedAt(now);

        return holding;
    }
}
