package com.fynxt.controller;

import com.fynxt.dto.AddToPortfolioRequest;
import com.fynxt.dto.PortfolioResponse;
import com.fynxt.dto.SecterOverlapResponse;
import com.fynxt.service.PortfolioService;
import com.fynxt.service.SectorOverlapService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traders")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final SectorOverlapService sectorOverlapService;

    public PortfolioController(
            PortfolioService portfolioService,
            SectorOverlapService sectorOverlapService) {

        this.portfolioService = portfolioService;
        this.sectorOverlapService = sectorOverlapService;
    }

    @GetMapping("/{traderId}/portfolio")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @PathVariable String traderId) {

        PortfolioResponse response =
                portfolioService.getPortfolio(traderId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{traderId}/sector-overlap")
    public ResponseEntity<SecterOverlapResponse> getSectorOverlap(
            @PathVariable String traderId) {

        SecterOverlapResponse response =
                sectorOverlapService.getSectorOverlap(traderId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{traderId}/portfolio/holdings")
    public ResponseEntity<PortfolioResponse> addToPortfolio(
            @PathVariable String traderId,
            @Valid @RequestBody AddToPortfolioRequest request) {

        PortfolioResponse response =
                portfolioService.addToPortfolio(
                        traderId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}