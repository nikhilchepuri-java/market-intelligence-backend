package com.marketintel.service;

import com.marketintel.model.Candle;
import com.marketintel.model.Quote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.*;
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

    @Override
    public List<Candle> candles(String symbol, String timeframe, Instant from, Instant to) {
        String s = symbol.toUpperCase();
        if (!"1D".equalsIgnoreCase(timeframe)) {
            throw new IllegalArgumentException("Demo provider currently supports timeframe=1D");
        }

        double base = prices.getOrDefault(s, 100.0);
        LocalDate start = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = to.atZone(ZoneOffset.UTC).toLocalDate();
        List<Candle> result = new ArrayList<>();

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            DayOfWeek dow = day.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            long dayNumber = day.toEpochDay();
            double drift = Math.sin(dayNumber * 0.17) * 0.012 + Math.sin(dayNumber * 0.047) * 0.008;
            double close = base * (1.0 + drift);
            double previous = result.isEmpty() ? close * 0.995 : result.get(result.size() - 1).close();
            double open = previous;
            double range = base * (0.006 + Math.abs(Math.sin(dayNumber * 0.31)) * 0.004);
            double high = Math.max(open, close) + range;
            double low = Math.min(open, close) - range;
            long volume = 500_000L + Math.round(500_000 * (1 + Math.sin(dayNumber * 0.23)) / 2);

            result.add(new Candle(
                    s, "1D",
                    day.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    round(open), round(high), round(low), round(close), volume
            ));
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}