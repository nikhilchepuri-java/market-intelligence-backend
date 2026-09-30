package com.marketintel.controller;

import com.marketintel.model.*;
import com.marketintel.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class MarketController {
    private final MarketDataProvider market;
    private final TechnicalService technical;
    private final SignalService signals;
    private final NewsService news;
    private final org.springframework.core.env.Environment environment;

    public MarketController(MarketDataProvider market, TechnicalService technical, SignalService signals,
                             NewsService news, org.springframework.core.env.Environment environment) {
        this.market = market;
        this.technical = technical;
        this.signals = signals;
        this.news = news;
        this.environment = environment;
    }

    @GetMapping("/quotes")
    public List<Quote> quotes(@RequestParam(defaultValue = "NDX,QQQ,SPY,AVGO,NVDA,AMD,TSLA") String symbols) {
        return market.quotes(Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    @GetMapping("/quotes/{symbol}")
    public Quote quote(@PathVariable String symbol) {
        return market.quote(symbol);
    }

    @GetMapping("/candles/{symbol}")
    public List<Candle> candles(@PathVariable String symbol,
                                @RequestParam(defaultValue = "1D") String timeframe,
                                @RequestParam(required = false) Instant from,
                                @RequestParam(required = false) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofDays(365)) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("from must be before to");
        return market.candles(symbol, timeframe, start, end);
    }

    @GetMapping("/technicals/{symbol}")
    public TechnicalSnapshot technical(@PathVariable String symbol) { return technical.snapshot(symbol); }

    @GetMapping("/levels/{symbol}")
    public SupportResistance levels(@PathVariable String symbol) {
        TechnicalSnapshot snapshot = technical.snapshot(symbol);
        return new SupportResistance(snapshot.support1(), snapshot.support2(), snapshot.resistance1(), snapshot.resistance2());
    }

    @GetMapping("/news/{symbol}")
    public List<NewsArticle> news(@PathVariable String symbol,
                                  @RequestParam(required = false) LocalDate from,
                                  @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(7) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("from must be before to");
        return news.news(symbol, start, end);
    }

    @GetMapping("/signals/{symbol}")
    public MarketSignal signal(@PathVariable String symbol) { return signals.signal(symbol); }

    @GetMapping("/data-status")
    public Map<String, Object> dataStatus() {
        String provider = environment.getProperty("market.provider", "unknown");
        String key = environment.getProperty("market.finnhub-api-key", "");
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", provider);
        status.put("mode", "finnhub".equalsIgnoreCase(provider) ? "realtime" : "demo");
        status.put("optionsProvider", environment.getProperty("market.options-provider", "unknown"));
        status.put("apiKeyConfigured", key != null && !key.isBlank());

        if (!"finnhub".equalsIgnoreCase(provider)) {
            status.put("quotesHealthy", true);
            status.put("candlesHealthy", true);
            status.put("newsHealthy", true);
            status.put("message", "Demo provider is active.");
            status.put("checkedAt", Instant.now().toString());
            return status;
        }

        status.put("quotesHealthy", probeQuote());
        status.put("candlesHealthy", probeCandles());
        status.put("newsHealthy", probeNews());
        status.put("message", statusMessage(status));
        status.put("checkedAt", Instant.now().toString());
        return status;
    }

    private boolean probeQuote() {
        try { return market.quote("QQQ").price() > 0; } catch (Exception e) { return false; }
    }

    private boolean probeCandles() {
        try {
            Instant end = Instant.now();
            return !market.candles("QQQ", "1D", end.minus(Duration.ofDays(10)), end).isEmpty();
        } catch (Exception e) { return false; }
    }

    private boolean probeNews() {
        try {
            return !news.news("QQQ", LocalDate.now().minusDays(2), LocalDate.now()).isEmpty();
        } catch (Exception e) { return false; }
    }

    private String statusMessage(Map<String, Object> status) {
        if (!(Boolean) status.get("apiKeyConfigured")) return "FINNHUB_API_KEY is not configured.";
        if (!(Boolean) status.get("quotesHealthy")) return "Finnhub quote check failed. Verify API key, entitlement, rate limit, and provider availability.";
        if (!(Boolean) status.get("candlesHealthy")) return "Finnhub candles check failed. Historical candles may require the appropriate Finnhub entitlement.";
        if (!(Boolean) status.get("newsHealthy")) return "Finnhub news check failed. Verify API access and news entitlement.";
        return "Finnhub quote, candle, and news checks are healthy.";
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP", "version", "V1.1"); }
}
