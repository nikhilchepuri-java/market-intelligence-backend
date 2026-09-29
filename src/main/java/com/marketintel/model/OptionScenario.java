package com.marketintel.model;

public record OptionScenario(
        String underlying,
        OptionType optionType,
        double strike,
        double underlyingPrice,
        double targetUnderlyingPrice,
        double impliedVolatility,
        double theoreticalPrice,
        double currentIntrinsicValue,
        double targetIntrinsicValue,
        double estimatedProfitLoss,
        double estimatedReturnPercent,
        double delta,
        double gamma,
        double theta,
        double vega,
        double rho) {
}
