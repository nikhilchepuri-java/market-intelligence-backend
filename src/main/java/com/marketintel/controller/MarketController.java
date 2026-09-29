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

    public MarketController(MarketDataProvider market, TechnicalService technical, SignalService signals, NewsService news) {
        this.market = market;
        this.technical = technical;
        this.signals = signals;
        this.news = news;
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
    public List<Candle> candles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1D") String timeframe,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofDays(365)) : from;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from must be before to");
        }
        return market.candles(symbol, timeframe, start, end);
    }

    @GetMapping("/technicals/{symbol}")
    public TechnicalSnapshot technical(@PathVariable String symbol) {
        return technical.snapshot(symbol);
    }

    @GetMapping("/levels/{symbol}")
    public SupportResistance levels(@PathVariable String symbol) {
        TechnicalSnapshot snapshot = technical.snapshot(symbol);
        return new SupportResistance(
                snapshot.support1(),
                snapshot.support2(),
                snapshot.resistance1(),
                snapshot.resistance2()
        );
    }

    @GetMapping("/news/{symbol}")
    public List<NewsArticle> news(
            @PathVariable String symbol,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(7) : from;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from must be before to");
        }
        return news.news(symbol, start, end);
    }

    @GetMapping("/signals/{symbol}")
    public MarketSignal signal(@PathVariable String symbol) {
        return signals.signal(symbol);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", "V1.1");
    }
}