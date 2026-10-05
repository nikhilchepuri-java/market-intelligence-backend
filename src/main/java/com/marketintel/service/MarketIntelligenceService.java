package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class MarketIntelligenceService {
    private final MarketDataProvider market;
    private final TechnicalService technical;
    private final NewsService news;
    private final OptionsProvider options;

    public MarketIntelligenceService(MarketDataProvider market, TechnicalService technical, NewsService news, OptionsProvider options) {
        this.market = market;
        this.technical = technical;
        this.news = news;
        this.options = options;
    }

    public MarketIntelligence analyze(String symbol) {
        String s = symbol.toUpperCase(Locale.ROOT);
        TechnicalSnapshot t = technical.snapshot(s);
        TechnicalSnapshot spy = technical.snapshot("SPY");
        TechnicalSnapshot qqq = technical.snapshot("QQQ");
        int marketScore = marketScore(spy, qqq);
        int technicalScore = Math.min(100, Math.max(0, (int) Math.round(t.conditionsMet() * 100.0 / t.totalConditions())));
        int newsScore = newsScore(s);
        List<OptionContract> contracts = bestContracts(s);
        int optionsScore = optionsScore(contracts);
        int score = (int) Math.round(marketScore * .25 + technicalScore * .35 + newsScore * .15 + optionsScore * .25);
        String bias = score >= 75 ? "BULLISH" : score <= 40 ? "BEARISH" : "NEUTRAL";
        String regime = marketRegime(spy, qqq);
        String strategy = chooseStrategy(bias, optionsScore, contracts);
        List<String> reasons = reasons(t, spy, qqq, newsScore, optionsScore);
        List<String> warnings = new ArrayList<>();
        if (contracts.isEmpty()) warnings.add("No usable option chain was returned.");
        if (t.rsi14() >= 70) warnings.add("RSI is overbought; avoid chasing extended upside.");
        if (t.rsi14() <= 30) warnings.add("RSI is oversold; downside momentum may be stretched.");
        if (marketScore < 50 && bias.equals("BULLISH")) warnings.add("Stock setup conflicts with the broader market regime.");
        if (marketScore > 65 && bias.equals("BEARISH")) warnings.add("Stock setup is weaker than the broader market.");
        String recommendation = score >= 80 ? "STRONG-BUY" : score >= 70 ? "BUY-WATCH" : score <= 35 ? "AVOID-BULLISH-TRADES" : "WATCH";
        String entry = bias.equals("BULLISH") ? String.format("Break and hold above %.2f with volume confirmation", t.resistance1()) :
                bias.equals("BEARISH") ? String.format("Lose %.2f and fail to reclaim it", t.support1()) :
                String.format("Wait for a break of %.2f or %.2f", t.resistance1(), t.support1());
        String stop = bias.equals("BULLISH") ? String.format("Close below %.2f", t.support1()) :
                bias.equals("BEARISH") ? String.format("Close above %.2f", t.resistance1()) : "No directional stop until breakout";
        String target = bias.equals("BULLISH") ? String.format("%.2f then %.2f", t.resistance1(), t.resistance2()) :
                bias.equals("BEARISH") ? String.format("%.2f then %.2f", t.support1(), t.support2()) : "Use breakout level as first target";
        return new MarketIntelligence(s, regime, bias, recommendation, score, technicalScore, newsScore, marketScore, optionsScore,
                strategy, entry, stop, target, 0, 0, riskLevel(contracts), reasons, warnings,
                "Rule-based market intelligence, not financial advice or a prediction. Validate liquidity, news, earnings, spreads and risk before trading.");
    }

    private int marketScore(TechnicalSnapshot spy, TechnicalSnapshot qqq) {
        int score = (spy.trend().equals("BULLISH") ? 55 : spy.trend().equals("MIXED") ? 30 : 10)
                + (qqq.trend().equals("BULLISH") ? 45 : qqq.trend().equals("MIXED") ? 25 : 5);
        return Math.min(100, score);
    }
    private String marketRegime(TechnicalSnapshot spy, TechnicalSnapshot qqq) {
        if (spy.trend().equals("BULLISH") && qqq.trend().equals("BULLISH")) return "STRONG_BULL";
        if (spy.trend().equals("BEARISH") && qqq.trend().equals("BEARISH")) return "STRONG_BEAR";
        if (spy.trend().equals("BULLISH") || qqq.trend().equals("BULLISH")) return "BULL";
        if (spy.trend().equals("BEARISH") || qqq.trend().equals("BEARISH")) return "BEAR";
        return "NEUTRAL";
    }
    private int newsScore(String symbol) {
        try {
            List<NewsArticle> items = news.news(symbol, LocalDate.now().minusDays(3), LocalDate.now());
            if (items.isEmpty()) return 50;
            long positive = items.stream().filter(n -> containsAny(n.headline()+" "+n.summary(), "beat","upgrade","growth","strong","raises","record")).count();
            long negative = items.stream().filter(n -> containsAny(n.headline()+" "+n.summary(), "miss","downgrade","cut","lawsuit","investigation","weak")).count();
            return Math.max(0, Math.min(100, 50 + (int)((positive-negative)*10)));
        } catch (Exception e) { return 50; }
    }
    private boolean containsAny(String text, String... words) {
        String x = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String w : words) if (x.contains(w)) return true;
        return false;
    }
    private List<OptionContract> bestContracts(String symbol) {
        try {
            List<LocalDate> exps = options.expirations(symbol);
            if (exps.isEmpty()) return List.of();
            LocalDate today = LocalDate.now();
            LocalDate expiry = exps.stream().filter(d -> !d.isBefore(today.plusDays(14))).findFirst().orElse(exps.get(0));
            return options.chain(symbol, expiry).contracts().stream()
                    .filter(c -> c.ask() > 0 && c.openInterest() >= 10)
                    .sorted(Comparator.comparingLong(OptionContract::openInterest).reversed()).limit(20).toList();
        } catch (Exception e) { return List.of(); }
    }
    private int optionsScore(List<OptionContract> contracts) {
        if (contracts.isEmpty()) return 50;
        double spreadPct = contracts.stream().mapToDouble(c -> {
            double mid = (c.ask()+c.bid())/2.0;
            return mid > 0 ? (c.ask()-c.bid())/mid : 1;
        }).average().orElse(1);
        return spreadPct <= .05 ? 90 : spreadPct <= .10 ? 75 : spreadPct <= .20 ? 55 : 30;
    }
    private String chooseStrategy(String bias, int optionsScore, List<OptionContract> contracts) {
        if (contracts.isEmpty()) return "NO-TRADE";
        if (optionsScore < 50) return "NO-TRADE";
        if (bias.equals("BULLISH")) return optionsScore >= 75 ? "BULL-CALL-SPREAD" : "CALL-WATCH";
        if (bias.equals("BEARISH")) return optionsScore >= 75 ? "BEAR-PUT-SPREAD" : "PUT-WATCH";
        return "NO-TRADE";
    }
    private List<String> reasons(TechnicalSnapshot t, TechnicalSnapshot spy, TechnicalSnapshot qqq, int newsScore, int optionsScore) {
        return List.of(String.format("Technical trend: %s (%d/%d conditions)", t.trend(), t.conditionsMet(), t.totalConditions()),
                String.format("RSI %.1f; relative volume %.2fx", t.rsi14(), t.relativeVolume20()),
                String.format("SPY regime: %s; QQQ regime: %s", spy.trend(), qqq.trend()),
                "Recent news score: "+newsScore+"/100", "Options liquidity score: "+optionsScore+"/100");
    }
    private String riskLevel(List<OptionContract> contracts) {
        if (contracts.isEmpty()) return "UNKNOWN";
        double spread = contracts.stream().mapToDouble(c -> c.ask()-c.bid()).average().orElse(0);
        return spread <= .10 ? "MODERATE" : "HIGH";
    }
}