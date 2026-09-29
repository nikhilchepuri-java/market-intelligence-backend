package com.marketintel.service;

import com.marketintel.model.Candle;
import com.marketintel.model.Quote;
import com.marketintel.model.TechnicalSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TechnicalServiceTest {

    @Test
    void calculatesHistoricalIndicatorsFromDailyCandles() {
        List<Candle> candles = new ArrayList<>();
        Instant start = Instant.parse("2025-01-01T00:00:00Z");

        for (int i = 0; i < 250; i++) {
            double close = 100.0 + i;
            candles.add(new Candle(
                    "TEST",
                    "1D",
                    start.plus(i, ChronoUnit.DAYS),
                    close - 1.0,
                    close + 2.0,
                    close - 2.0,
                    close,
                    i == 249 ? 3_000L : 1_000L
            ));
        }

        MarketDataProvider provider = new MarketDataProvider() {
            @Override
            public Quote quote(String symbol) {
                return new Quote(symbol, 349.0, 0.0, 0.0, 351.0, 347.0, 2900L, start.plus(249, ChronoUnit.DAYS).toString());
            }

            @Override
            public List<Quote> quotes(List<String> symbols) {
                return symbols.stream().map(this::quote).toList();
            }

            @Override
            public List<Candle> candles(String symbol, String timeframe, Instant from, Instant to) {
                return candles;
            }
        };

        TechnicalSnapshot snapshot = new TechnicalService(provider).snapshot("TEST");

        assertEquals(349.0, snapshot.price());
        assertEquals(339.5, snapshot.sma20(), 0.000001);
        assertEquals(324.5, snapshot.sma50(), 0.000001);
        assertEquals(249.5, snapshot.sma200(), 0.000001);
        assertEquals(100.0, snapshot.rsi14(), 0.000001);
        assertEquals(4.0, snapshot.atr14(), 0.000001);

        assertTrue(snapshot.ema9() > snapshot.ema21());
        assertTrue(snapshot.ema21() > snapshot.ema50());

        System.out.println("price = " + snapshot.price());
        System.out.println("sma20 = " + snapshot.sma20());
        System.out.println("sma50 = " + snapshot.sma50());
        System.out.println("sma200 = " + snapshot.sma200());
        System.out.println("rsi14 = " + snapshot.rsi14());
        System.out.println("ema21 = " + snapshot.ema21());
        System.out.println("relativeVolume20 = " + snapshot.relativeVolume20());
        System.out.println("volume = " + snapshot.volume());
        System.out.println("averageVolume20 = " + snapshot.averageVolume20());
        System.out.println("conditionsMet = " + snapshot.conditionsMet());
        assertTrue(snapshot.relativeVolume20() >= 1.0);
        assertEquals("BULLISH", snapshot.trend());
        assertEquals(6, snapshot.conditionsMet());
        assertEquals(6, snapshot.totalConditions());
    }
}
