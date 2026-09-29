package com.marketintel.service;

import com.marketintel.model.Candle;
import com.marketintel.model.Quote;

import java.time.Instant;
import java.util.List;

public interface MarketDataProvider {
    Quote quote(String symbol);

    List<Quote> quotes(List<String> symbols);

    List<Candle> candles(String symbol, String timeframe, Instant from, Instant to);
}