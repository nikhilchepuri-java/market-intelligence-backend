package com.marketintel.service;

import com.marketintel.model.Quote;

import java.util.List;

public interface MarketDataProvider {
    Quote quote(String symbol);

    List<Quote> quotes(List<String> symbols);
}
