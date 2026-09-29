package com.marketintel.service;

import com.marketintel.model.Candle;
import com.marketintel.model.SupportResistance;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupportResistanceServiceTest {

    @Test
    void findsNearestSwingLevelsAroundPrice() {
        double[] lows = {100, 98, 95, 90, 96, 94, 80, 85, 90, 92};
        double[] highs = {105, 110, 115, 120, 116, 118, 130, 125, 110, 108};
        List<Candle> candles = new ArrayList<>();

        for (int i = 0; i < lows.length; i++) {
            double close = (lows[i] + highs[i]) / 2.0;
            candles.add(new Candle(
                    "TEST",
                    "1D",
                    Instant.parse("2025-01-01T00:00:00Z").plusSeconds(i * 86400L),
                    close,
                    highs[i],
                    lows[i],
                    close,
                    1_000L
            ));
        }

        SupportResistance levels = new SupportResistanceService().calculate(candles, 100.0);

        assertEquals(90.0, levels.support1());
        assertEquals(80.0, levels.support2());
        assertEquals(120.0, levels.resistance1());
        assertEquals(130.0, levels.resistance2());
    }

    @Test
    void usesPriceBasedFallbackWhenNoSwingLevelsExist() {
        SupportResistance levels = new SupportResistanceService().calculate(
                List.of(),
                100.0
        );

        assertEquals(98.0, levels.support1(), 0.000001);
        assertEquals(95.06, levels.support2(), 0.000001);
        assertEquals(102.0, levels.resistance1(), 0.000001);
        assertEquals(105.06, levels.resistance2(), 0.000001);
    }
}
