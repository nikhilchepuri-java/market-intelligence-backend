package com.marketintel.controller;

import com.marketintel.model.*;
import com.marketintel.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class MarketController {
    private final MarketDataProvider market;
    private final TechnicalService technical;
    private final SignalService signals;

    public MarketController(MarketDataProvider market, TechnicalService technical, SignalService signals) {
        this.market = market;
        this.technical = technical;
        this.signals = signals;
    }

    @GetMapping("/quotes")
    public List<Quote> quotes(@RequestParam(defaultValue = "NDX,QQQ,SPY,AVGO,NVDA,AMD,TSLA") String symbols) {
        return market.quotes(Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    @GetMapping("/quotes/{symbol}")
    public Quote quote(@PathVariable String symbol) {
        return market.quote(symbol);
    }

    @GetMapping("/technicals/{symbol}")
    public TechnicalSnapshot technical(@PathVariable String symbol) {
        return technical.snapshot(symbol);
    }

    @GetMapping("/signals/{symbol}")
    public MarketSignal signal(@PathVariable String symbol) {
        return signals.signal(symbol);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", "V1");
    }
}
