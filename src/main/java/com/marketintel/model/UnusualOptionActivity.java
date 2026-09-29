package com.marketintel.model;

import java.time.LocalDate;

public record UnusualOptionActivity(
        String symbol,
        String underlying,
        OptionType optionType,
        LocalDate expiration,
        double strike,
        long volume,
        long openInterest,
        double volumeOpenInterestRatio,
        double impliedVolatility,
        double premium,
        String reason) {
}
