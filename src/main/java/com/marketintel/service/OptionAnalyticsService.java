package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class OptionAnalyticsService {
    private final OptionsProvider provider;

    public OptionAnalyticsService(OptionsProvider provider) {
        this.provider = provider;
    }

    public List<LocalDate> expirations(String underlying) {
        return provider.expirations(underlying);
    }

    public OptionChain chain(String underlying, LocalDate expiration) {
        return provider.chain(underlying, expiration);
    }

    public List<UnusualOptionActivity> unusual(String underlying, LocalDate expiration) {
        return chain(underlying, expiration).contracts().stream()
                .filter(c -> c.volume() > 0)
                .map(c -> {
                    double ratio = c.openInterest() == 0 ? Double.POSITIVE_INFINITY
                            : (double) c.volume() / c.openInterest();
                    double premium = ((c.bid() + c.ask()) / 2.0) * c.volume() * c.contractSize();
                    return new UnusualOptionActivity(c.symbol(), c.underlying(), c.optionType(),
                            c.expiration(), c.strike(), c.volume(), c.openInterest(), ratio,
                            c.impliedVolatility(), premium, reason(c, ratio));
                })
                .filter(x -> x.volumeOpenInterestRatio() >= 1.5 || x.volume() >= 5000)
                .sorted(Comparator.comparingDouble(UnusualOptionActivity::volumeOpenInterestRatio).reversed())
                .toList();
    }

    private String reason(OptionContract c, double ratio) {
        if (ratio >= 3) return "Volume is at least 3x open interest";
        if (ratio >= 1.5) return "Volume exceeds 1.5x open interest";
        return "High option volume";
    }

    public OptionScenario scenario(OptionScenarioRequest request) {
        double t = Math.max(1.0 / 365.0,
                ChronoUnit.DAYS.between(LocalDate.now(), request.expiration()) / 365.0);
        double theoretical = blackScholes(
                request.targetUnderlyingPrice(), request.strike(), t,
                request.riskFreeRate(), request.impliedVolatility(), request.optionType());

        double currentTheoretical = blackScholes(
                request.underlyingPrice(), request.strike(), t,
                request.riskFreeRate(), request.impliedVolatility(), request.optionType());

        double pnl = (theoretical - currentTheoretical) * request.contractSize();
        double cost = currentTheoretical * request.contractSize();
        double returnPct = cost == 0 ? 0 : pnl / cost * 100;

        Greeks g = greeks(request.underlyingPrice(), request.strike(), t,
                request.riskFreeRate(), request.impliedVolatility(), request.optionType());

        double currentIntrinsic = intrinsic(request.underlyingPrice(), request.strike(), request.optionType());
        double targetIntrinsic = intrinsic(request.targetUnderlyingPrice(), request.strike(), request.optionType());

        return new OptionScenario(request.underlying(), request.optionType(), request.strike(),
                request.underlyingPrice(), request.targetUnderlyingPrice(), request.impliedVolatility(),
                theoretical, currentIntrinsic, targetIntrinsic, pnl, returnPct,
                g.delta(), g.gamma(), g.theta(), g.vega(), g.rho());
    }

    private double intrinsic(double s, double k, OptionType type) {
        return Math.max(type == OptionType.CALL ? s - k : k - s, 0);
    }

    private double blackScholes(double s, double k, double t, double r, double sigma, OptionType type) {
        double d1 = (Math.log(s / k) + (r + sigma * sigma / 2) * t) / (sigma * Math.sqrt(t));
        double d2 = d1 - sigma * Math.sqrt(t);
        if (type == OptionType.CALL) {
            return s * normalCdf(d1) - k * Math.exp(-r * t) * normalCdf(d2);
        }
        return k * Math.exp(-r * t) * normalCdf(-d2) - s * normalCdf(-d1);
    }

    private Greeks greeks(double s, double k, double t, double r, double sigma, OptionType type) {
        double sqrtT = Math.sqrt(t);
        double d1 = (Math.log(s / k) + (r + sigma * sigma / 2) * t) / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;
        double pdf = normalPdf(d1);
        double gamma = pdf / (s * sigma * sqrtT);
        double vega = s * pdf * sqrtT / 100.0;
        double delta = type == OptionType.CALL ? normalCdf(d1) : normalCdf(d1) - 1;
        double thetaAnnual = type == OptionType.CALL
                ? -(s * pdf * sigma) / (2 * sqrtT) - r * k * Math.exp(-r * t) * normalCdf(d2)
                : -(s * pdf * sigma) / (2 * sqrtT) + r * k * Math.exp(-r * t) * normalCdf(-d2);
        double theta = thetaAnnual / 365.0;
        double rho = type == OptionType.CALL
                ? k * t * Math.exp(-r * t) * normalCdf(d2) / 100.0
                : -k * t * Math.exp(-r * t) * normalCdf(-d2) / 100.0;
        return new Greeks(delta, gamma, theta, vega, rho);
    }

    private double normalPdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
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

    private record Greeks(double delta, double gamma, double theta, double vega, double rho) {}
}
