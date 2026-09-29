package com.marketintel.model;

public record TechnicalSnapshot(
        String symbol,
        double price,
        double sma20,
        double sma50,
        double sma200,
        double ema9,
        double ema21,
        double ema50,
        double rsi14,
        double atr14,
        long volume,
        double averageVolume20,
        double relativeVolume20,
        double support1,
        double support2,
        double resistance1,
        double resistance2,
        String trend,
        int conditionsMet,
        int totalConditions) {
}
