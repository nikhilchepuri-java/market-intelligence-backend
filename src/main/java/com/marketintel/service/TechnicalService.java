package com.marketintel.service;

import com.marketintel.model.Candle;
import com.marketintel.model.Quote;
import com.marketintel.model.TechnicalSnapshot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TechnicalService {
    private static final int RSI_PERIOD = 14;
    private static final int ATR_PERIOD = 14;
    private static final int SUPPORT_RESISTANCE_LOOKBACK = 120;
    private static final int SWING_WINDOW = 2;

    private final MarketDataProvider provider;

    public TechnicalService(MarketDataProvider provider) {
        this.provider = provider;
    }

    public TechnicalSnapshot snapshot(String symbol) {
        String normalized = symbol.toUpperCase(Locale.ROOT);
        Quote quote = provider.quote(normalized);

        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(450));
        List<Candle> candles = new ArrayList<>(provider.candles(normalized, "1D", from, to));
        candles.sort(Comparator.comparing(Candle::timestamp));

        if (candles.isEmpty()) {
            throw new IllegalStateException("No historical candles available for " + normalized);
        }

        List<Double> closes = candles.stream().map(Candle::close).toList();
        List<Double> highs = candles.stream().map(Candle::high).toList();
        List<Double> lows = candles.stream().map(Candle::low).toList();

        double price = quote.price() > 0 ? quote.price() : closes.get(closes.size() - 1);
        double sma20 = sma(closes, 20);
        double sma50 = sma(closes, 50);
        double sma200 = sma(closes, 200);
        double ema9 = ema(closes, 9);
        double ema21 = ema(closes, 21);
        double ema50 = ema(closes, 50);
        double rsi14 = rsi(closes, RSI_PERIOD);
        double atr14 = atr(candles, ATR_PERIOD);

        int volumePeriod = Math.min(20, candles.size());
        double averageVolume20 = candles.subList(candles.size() - volumePeriod, candles.size())
                .stream()
                .mapToLong(Candle::volume)
                .average()
                .orElse(0.0);
        long volume = candles.get(candles.size() - 1).volume();
        double relativeVolume20 = averageVolume20 > 0 ? volume / averageVolume20 : 0.0;

        double[] levels = supportResistance(candles, price);
        double support1 = levels[0];
        double support2 = levels[1];
        double resistance1 = levels[2];
        double resistance2 = levels[3];

        int conditions = 0;
        int totalConditions = 6;
        if (price > sma20) conditions++;
        if (price > sma50) conditions++;
        if (sma50 > sma200) conditions++;
        if (rsi14 >= 50) conditions++;
        if (price > ema21) conditions++;
        if (relativeVolume20 >= 1.0) conditions++;

        String trend;
        if (price > sma20 && price > sma50 && sma50 > sma200 && price > ema21) {
            trend = "BULLISH";
        } else if (price < sma20 && price < sma50 && sma50 < sma200 && price < ema21) {
            trend = "BEARISH";
        } else {
            trend = "MIXED";
        }

        return new TechnicalSnapshot(
                normalized, price,
                sma20, sma50, sma200,
                ema9, ema21, ema50,
                rsi14, atr14,
                volume, averageVolume20, relativeVolume20,
                support1, support2, resistance1, resistance2,
                trend, conditions, totalConditions
        );
    }

    private double sma(List<Double> values, int period) {
        if (values.size() < period) {
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        return values.subList(values.size() - period, values.size())
                .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double ema(List<Double> values, int period) {
        if (values.isEmpty()) return 0.0;
        double alpha = 2.0 / (period + 1.0);
        int seedStart = Math.max(0, values.size() - period);
        double ema = values.subList(seedStart, values.size())
                .stream().mapToDouble(Double::doubleValue).average().orElse(values.get(0));
        for (int i = seedStart + period; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1.0 - alpha) * ema;
        }
        if (values.size() <= period) {
            ema = values.get(0);
            for (int i = 1; i < values.size(); i++) {
                ema = alpha * values.get(i) + (1.0 - alpha) * ema;
            }
        }
        return ema;
    }

    private double rsi(List<Double> closes, int period) {
        if (closes.size() <= period) return 50.0;

        double gain = 0.0;
        double loss = 0.0;
        for (int i = 1; i <= period; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change >= 0) gain += change;
            else loss -= change;
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        for (int i = period + 1; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            double currentGain = Math.max(change, 0.0);
            double currentLoss = Math.max(-change, 0.0);
            avgGain = ((avgGain * (period - 1)) + currentGain) / period;
            avgLoss = ((avgLoss * (period - 1)) + currentLoss) / period;
        }

        if (avgLoss == 0.0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    private double atr(List<Candle> candles, int period) {
        if (candles.size() < 2) return 0.0;

        List<Double> trueRanges = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);
            double tr = Math.max(
                    current.high() - current.low(),
                    Math.max(
                            Math.abs(current.high() - previous.close()),
                            Math.abs(current.low() - previous.close())
                    )
            );
            trueRanges.add(tr);
        }

        if (trueRanges.size() < period) {
            return trueRanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        double atr = trueRanges.subList(0, period).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);

        for (int i = period; i < trueRanges.size(); i++) {
            atr = ((atr * (period - 1)) + trueRanges.get(i)) / period;
        }
        return atr;
    }

    private double[] supportResistance(List<Candle> candles, double price) {
        int start = Math.max(0, candles.size() - SUPPORT_RESISTANCE_LOOKBACK);
        List<Double> supports = new ArrayList<>();
        List<Double> resistances = new ArrayList<>();

        for (int i = start + SWING_WINDOW; i < candles.size() - SWING_WINDOW; i++) {
            double low = candles.get(i).low();
            double high = candles.get(i).high();
            boolean swingLow = true;
            boolean swingHigh = true;

            for (int j = i - SWING_WINDOW; j <= i + SWING_WINDOW; j++) {
                if (j == i) continue;
                if (candles.get(j).low() <= low) swingLow = false;
                if (candles.get(j).high() >= high) swingHigh = false;
            }

            if (swingLow && low < price) supports.add(low);
            if (swingHigh && high > price) resistances.add(high);
        }

        supports.sort(Comparator.reverseOrder());
        resistances.sort(Comparator.naturalOrder());

        double support1 = supports.size() > 0 ? supports.get(0) : price * 0.98;
        double support2 = supports.size() > 1 ? supports.get(1) : support1 * 0.97;
        double resistance1 = resistances.size() > 0 ? resistances.get(0) : price * 1.02;
        double resistance2 = resistances.size() > 1 ? resistances.get(1) : resistance1 * 1.03;

        return new double[]{support1, support2, resistance1, resistance2};
    }
}
