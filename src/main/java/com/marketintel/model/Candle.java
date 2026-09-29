package com.marketintel.model;

import java.time.Instant;

public record Candle(
        String symbol,
        String timeframe,
        Instant timestamp,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
}