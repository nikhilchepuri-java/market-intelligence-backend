package com.marketintel.model;

public record MarketSignal(String symbol, String signal, String regime, int score, String trigger, String invalidation,
                           String upsideScenario, String downsideScenario, String explanation) {
}
