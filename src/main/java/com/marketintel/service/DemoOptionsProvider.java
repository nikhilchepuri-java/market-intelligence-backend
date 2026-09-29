package com.marketintel.service;

import com.marketintel.model.OptionChain;
import com.marketintel.model.OptionContract;
import com.marketintel.model.OptionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "market.options-provider", havingValue = "demo", matchIfMissing = true)
public class DemoOptionsProvider implements OptionsProvider {
    private final MarketDataProvider market;

    public DemoOptionsProvider(MarketDataProvider market) {
        this.market = market;
    }

    @Override
    public List<LocalDate> expirations(String underlying) {
        LocalDate today = LocalDate.now();
        return List.of(
                nextFriday(today),
                nextFriday(today.plusWeeks(1)),
                nextFriday(today.plusWeeks(2)),
                nextFriday(today.plusWeeks(4))
        );
    }

    @Override
    public OptionChain chain(String underlying, LocalDate expiration) {
        double spot = market.quote(underlying).price();
        long days = Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), expiration));
        double t = days / 365.0;
        List<OptionContract> contracts = new ArrayList<>();
        for (int i = -8; i <= 8; i++) {
            double strike = round(spot + i * Math.max(1.0, spot * 0.025));
            for (OptionType type : OptionType.values()) {
                double iv = 0.28 + Math.abs(i) * 0.006 + (type == OptionType.PUT ? 0.015 : 0);
                double mid = blackScholes(spot, strike, t, 0.04, iv, type);
                long volume = 250L + Math.max(0, 1800L - Math.abs(i) * 150L) + (type == OptionType.CALL ? 120 : 0);
                long oi = 700L + Math.abs(i) * 180L;
                contracts.add(new OptionContract(
                        underlying.toUpperCase() + "-" + expiration + "-" + type + "-" + strike,
                        underlying.toUpperCase(), type, expiration, strike,
                        round(mid * 0.98), round(mid * 1.02), round(mid),
                        volume, oi, iv,
                        0, 0, 0, 0, 0, 100));
            }
        }
        return new OptionChain(underlying.toUpperCase(), expiration, contracts);
    }

    private LocalDate nextFriday(LocalDate date) {
        int days = (5 - date.getDayOfWeek().getValue() + 7) % 7;
        if (days == 0) days = 7;
        return date.plusDays(days);
    }

    private double blackScholes(double s, double k, double t, double r, double sigma, OptionType type) {
        if (t <= 0) return Math.max(type == OptionType.CALL ? s - k : k - s, 0);
        double d1 = (Math.log(s / k) + (r + sigma * sigma / 2) * t) / (sigma * Math.sqrt(t));
        double d2 = d1 - sigma * Math.sqrt(t);
        double nd1 = normalCdf(type == OptionType.CALL ? d1 : -d1);
        double nd2 = normalCdf(type == OptionType.CALL ? d2 : -d2);
        if (type == OptionType.CALL) return s * normalCdf(d1) - k * Math.exp(-r * t) * normalCdf(d2);
        return k * Math.exp(-r * t) * normalCdf(-d2) - s * normalCdf(-d1);
    }

    private double normalCdf(double x) {
        return 0.5 * (1 + erf(x / Math.sqrt(2)));
    }

    private double erf(double x) {
        double sign = x < 0 ? -1 : 1;
        x = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * x);
        double y = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return sign * y;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
