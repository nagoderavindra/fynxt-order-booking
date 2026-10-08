package com.fynxt.service;

import com.fynxt.dto.AddToPortfolioRequest;
import com.fynxt.dto.PortfolioResponse;

public interface PortfolioService {

    PortfolioResponse getPortfolio(String traderId);

    PortfolioResponse addToPortfolio(
            String traderId,
            AddToPortfolioRequest request

    );

}