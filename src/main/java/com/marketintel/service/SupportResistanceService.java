package com.marketintel.service;

import com.marketintel.model.Candle;
import com.marketintel.model.SupportResistance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SupportResistanceService {
    private static final int LOOKBACK = 120;
    private static final int SWING_WINDOW = 2;

    public SupportResistance calculate(List<Candle> candles, double price) {
        if (candles == null || candles.isEmpty()) {
            return fallback(price);
        }

        int start = Math.max(0, candles.size() - LOOKBACK);
        List<Double> supports = new ArrayList<>();
        List<Double> resistances = new ArrayList<>();

        for (int i = start + SWING_WINDOW; i < candles.size() - SWING_WINDOW; i++) {
            double low = candles.get(i).low();
            double high = candles.get(i).high();
            boolean swingLow = true;
            boolean swingHigh = true;

            for (int j = i - SWING_WINDOW; j <= i + SWING_WINDOW; j++) {
                if (j == i) {
                    continue;
                }
                if (candles.get(j).low() <= low) {
                    swingLow = false;
                }
                if (candles.get(j).high() >= high) {
                    swingHigh = false;
                }
            }

            if (swingLow && low < price) {
                supports.add(low);
            }
            if (swingHigh && high > price) {
                resistances.add(high);
            }
        }

        supports.sort(Comparator.reverseOrder());
        resistances.sort(Comparator.naturalOrder());

        double support1 = supports.isEmpty() ? price * 0.98 : supports.get(0);
        double support2 = supports.size() < 2 ? support1 * 0.97 : supports.get(1);
        double resistance1 = resistances.isEmpty() ? price * 1.02 : resistances.get(0);
        double resistance2 = resistances.size() < 2 ? resistance1 * 1.03 : resistances.get(1);

        return new SupportResistance(support1, support2, resistance1, resistance2);
    }

    private SupportResistance fallback(double price) {
        double support1 = price * 0.98;
        double support2 = support1 * 0.97;
        double resistance1 = price * 1.02;
        double resistance2 = resistance1 * 1.03;
        return new SupportResistance(support1, support2, resistance1, resistance2);
    }
}
