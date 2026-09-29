package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

@Service
public class SignalService {
    private final TechnicalService technical;

    public SignalService(TechnicalService technical) {
        this.technical = technical;
    }

    public MarketSignal signal(String symbol) {
        TechnicalSnapshot t = technical.snapshot(symbol);
        String signal = t.trend().equals("BULLISH") && t.conditionsMet() >= 3 ? "WATCH-BULLISH" : t.trend().equals("BEARISH") ? "WATCH-BEARISH" : "WATCH";
        int score = (int) Math.round(t.conditionsMet() * 100.0 / t.totalConditions());
        return new MarketSignal(t.symbol(), signal, t.trend(), score,
                String.format("Break above %.2f with volume confirmation", t.resistance1()),
                String.format("Lose %.2f and fail to reclaim", t.support1()),
                String.format("%.2f -> %.2f", t.resistance1(), t.resistance2()),
                String.format("%.2f -> %.2f", t.support1(), t.support2()),
                "V1.1 signal is rule-based and transparent; it uses historical indicators and is not a prediction or investment recommendation.");
    }
}
