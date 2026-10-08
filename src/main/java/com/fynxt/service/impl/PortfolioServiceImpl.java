package com.fynxt.service.impl;

import com.fynxt.dto.AddToPortfolioRequest;
import com.fynxt.dto.PortfolioResponse;
import com.fynxt.entity.PortfolioHolding;
import com.fynxt.exception.BusinessException;
import com.fynxt.exception.ResourceNotFoundException;
import com.fynxt.repository.PortfolioHoldingRepository;
import com.fynxt.repository.TraderRepository;
import com.fynxt.service.PortfolioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioHoldingRepository portfolioHoldingRepository;
    private final TraderRepository traderRepository;

    public PortfolioServiceImpl(
            PortfolioHoldingRepository portfolioHoldingRepository,
            TraderRepository traderRepository) {

        this.portfolioHoldingRepository = portfolioHoldingRepository;
        this.traderRepository = traderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(String traderId) {

        traderRepository.findById(traderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trader not found: " + traderId
                        )
                );

        List<PortfolioHolding> holdings =
                portfolioHoldingRepository.findAllByTraderId(traderId);

        Map<String, Integer> positions = new LinkedHashMap<>();
        Map<String, Integer> sectorBreakdown = new LinkedHashMap<>();

        for (PortfolioHolding holding : holdings) {

            if (holding.getQuantity() <= 0) {
                continue;
            }

            positions.merge(
                    holding.getStock(),
                    holding.getQuantity(),
                    Integer::sum
            );

            sectorBreakdown.merge(
                    holding.getSector(),
                    holding.getQuantity(),
                    Integer::sum
            );
        }

        return new PortfolioResponse(
                traderId,
                positions,
                sectorBreakdown
        );
    }

    @Override
    @Transactional
    public PortfolioResponse addToPortfolio(
            String traderId,
            AddToPortfolioRequest request) {

        // Lock trader to handle concurrent portfolio updates
        traderRepository.findByIdForUpdate(traderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trader not found: " + traderId
                        )
                );

        // Lock existing holding
        PortfolioHolding holding =
                portfolioHoldingRepository
                        .findByTraderIdAndStockForUpdate(
                                traderId,
                                request.getStock()
                        )
                        .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (holding == null) {

            // Create new holding
            holding = new PortfolioHolding();

            holding.setTraderId(traderId);
            holding.setStock(request.getStock());
            holding.setSector(request.getSector());
            holding.setQuantity(request.getQuantity());
            holding.setReservedQuantity(0);
            holding.setCreatedAt(now);
            holding.setUpdatedAt(now);

        } else {

            // Same stock should have same sector
            if (!holding.getSector()
                    .equalsIgnoreCase(request.getSector())) {

                throw new BusinessException(
                        "Sector mismatch for stock: "
                                + request.getStock()
                );
            }

            // Add quantity to existing holding
            holding.setQuantity(
                    holding.getQuantity() + request.getQuantity()
            );

            holding.setUpdatedAt(now);
        }

        portfolioHoldingRepository.save(holding);

        return getPortfolio(traderId);
    }
}