package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TechnicalService {
    private final MarketDataProvider provider;

    public TechnicalService(MarketDataProvider provider) {
        this.provider = provider;
    }

    public TechnicalSnapshot snapshot(String symbol) {
        Quote q = provider.quote(symbol);
        double p = q.price();
        // V1 intentionally uses transparent price-derived levels until historical candles are connected.
        double sma20 = p * 0.992, sma50 = p * 0.975, rsi = 55.0;
        double support1 = p * 0.985, support2 = p * 0.955, resistance1 = p * 1.015, resistance2 = p * 1.04;
        int conditions = 0;
        if (p > sma20) conditions++;
        if (p > sma50) conditions++;
        if (rsi > 50) conditions++;
        if (q.changePercent() > 0) conditions++;
        String trend = p > sma20 && p > sma50 ? "BULLISH" : p < sma20 && p < sma50 ? "BEARISH" : "MIXED";
        return new TechnicalSnapshot(symbol.toUpperCase(), p, sma20, sma50, rsi, support1, support2, resistance1, resistance2, trend, conditions, 4);
    }
}
