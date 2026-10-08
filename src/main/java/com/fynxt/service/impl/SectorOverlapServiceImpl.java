package com.fynxt.service.impl;

import com.fynxt.dto.SecterOverlapResponse;
import com.fynxt.entity.PortfolioHolding;
import com.fynxt.exception.ResourceNotFoundException;
import com.fynxt.repository.PortfolioHoldingRepository;
import com.fynxt.repository.TraderRepository;
import com.fynxt.service.SectorOverlapCalculator;
import com.fynxt.service.SectorOverlapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SectorOverlapServiceImpl implements SectorOverlapService {

    private final TraderRepository traderRepository;
    private final PortfolioHoldingRepository portfolioHoldingRepository;
    private final SectorOverlapCalculator sectorOverlapCalculator;

    public SectorOverlapServiceImpl(
            TraderRepository traderRepository,
            PortfolioHoldingRepository portfolioHoldingRepository) {

        this.traderRepository = traderRepository;
        this.portfolioHoldingRepository = portfolioHoldingRepository;
        this.sectorOverlapCalculator = new SectorOverlapCalculator();
    }

    @Override
    @Transactional(readOnly = true)
    public SecterOverlapResponse getSectorOverlap(String traderId) {

        // Check whether trader exists
        traderRepository.findById(traderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trader not found: " + traderId
                        )
                );

        // Get trader's portfolio holdings
        Set<String> portfolioStocks =
                portfolioHoldingRepository
                        .findAllByTraderId(traderId)
                        .stream()
                        .filter(holding -> holding.getQuantity() > 0)
                        .map(PortfolioHolding::getStock)
                        .collect(Collectors.toSet());

        // Perform pure Java overlap calculation
        return sectorOverlapCalculator.calculate(portfolioStocks);
    }
}