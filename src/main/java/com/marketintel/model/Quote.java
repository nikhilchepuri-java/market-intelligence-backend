package com.marketintel.model;

public record Quote(String symbol, double price, double change, double changePercent, double high, double low,
                    long volume, String timestamp) {
}
