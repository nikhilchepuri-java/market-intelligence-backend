package com.marketintel.model;

import java.time.LocalDate;

public record OptionContract(
        String symbol,
        String underlying,
        OptionType optionType,
        LocalDate expiration,
        double strike,
        double bid,
        double ask,
        double last,
        long volume,
        long openInterest,
        double impliedVolatility,
        double delta,
        double gamma,
        double theta,
        double vega,
        double rho,
        double contractSize) {
}
