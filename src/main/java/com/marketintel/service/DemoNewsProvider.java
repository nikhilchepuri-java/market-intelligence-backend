package com.marketintel.service;

import com.marketintel.model.NewsArticle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@ConditionalOnProperty(name = "market.provider", havingValue = "demo", matchIfMissing = true)
public class DemoNewsProvider implements NewsProvider {
    @Override
    public List<NewsArticle> news(String symbol, LocalDate from, LocalDate to) {
        String s = symbol.toUpperCase();
        Instant published = to.atStartOfDay(ZoneOffset.UTC).toInstant();
        return List.of(
                new NewsArticle(s, Math.abs(s.hashCode()), published,
                        s + " market intelligence demo article",
                        "Demo Feed",
                        "Demo news is enabled when MARKET_PROVIDER=demo. Configure FINNHUB_API_KEY and MARKET_PROVIDER=finnhub for live company news.",
                        "",
                        "",
                        "demo",
                        s)
        );
    }
}
