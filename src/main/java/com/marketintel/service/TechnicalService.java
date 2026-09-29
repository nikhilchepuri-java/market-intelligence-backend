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
    private final MarketDataProvider provider;
    private final SupportResistanceService supportResistanceService;

    public TechnicalService(MarketDataProvider provider, SupportResistanceService supportResistanceService) {
        this.provider = provider;
        this.supportResistanceService = supportResistanceService;
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

        var levels = supportResistanceService.calculate(candles, price);
        double support1 = levels.support1();
        double support2 = levels.support2();
        double resistance1 = levels.resistance1();
        double resistance2 = levels.resistance2();

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
        int seedCount = Math.min(period, values.size());
        double ema = values.subList(0, seedCount)
                .stream().mapToDouble(Double::doubleValue).average().orElse(values.get(0));
        for (int i = seedCount; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1.0 - alpha) * ema;
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

}