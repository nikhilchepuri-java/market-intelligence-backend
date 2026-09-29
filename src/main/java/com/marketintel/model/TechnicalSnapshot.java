package com.marketintel.model;

public record TechnicalSnapshot(String symbol, double price, double sma20, double sma50, double rsi14,
                                double support1, double support2, double resistance1, double resistance2,
                                String trend, int conditionsMet, int totalConditions) {
}
