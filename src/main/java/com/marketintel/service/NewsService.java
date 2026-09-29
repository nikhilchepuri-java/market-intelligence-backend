package com.marketintel.service;

import com.marketintel.model.NewsArticle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NewsService {
    private final NewsProvider provider;

    public NewsService(NewsProvider provider) {
        this.provider = provider;
    }

    public List<NewsArticle> news(String symbol, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        return provider.news(symbol, from, to);
    }
}
