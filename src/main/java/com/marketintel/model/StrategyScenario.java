package com.marketintel.model;

import java.util.List;

public record StrategyScenario(
        String underlying,
        double underlyingPrice,
        double netPremium,
        double maxProfit,
        double maxLoss,
        List<Double> breakevenPrices,
        double delta,
        double gamma,
        double theta,
        double vega,
        double rho,
        List<StrategyPayoffPoint> payoff) {}
