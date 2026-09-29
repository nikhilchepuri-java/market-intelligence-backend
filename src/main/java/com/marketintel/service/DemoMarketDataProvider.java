package com.marketintel.service;

import com.marketintel.model.Quote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name = "market.provider", havingValue = "demo", matchIfMissing = true)
public class DemoMarketDataProvider implements MarketDataProvider {
    private final Map<String, Double> prices = Map.of("NDX", 30200.0, "QQQ", 735.0, "SPY", 675.0, "AVGO", 357.8, "NVDA", 180.0, "AMD", 160.0, "TSLA", 420.0);

    public Quote quote(String symbol) {
        double p = prices.getOrDefault(symbol.toUpperCase(), 100.0);
        double ch = p * 0.008;
        return new Quote(symbol.toUpperCase(), p, ch, 0.8, p * 1.012, p * 0.988, 1_000_000, Instant.now().toString());
    }

    public List<Quote> quotes(List<String> symbols) {
        return symbols.stream().map(this::quote).toList();
    }
}
