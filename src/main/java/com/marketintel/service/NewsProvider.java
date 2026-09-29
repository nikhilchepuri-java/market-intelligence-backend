package com.marketintel.service;

import com.marketintel.model.NewsArticle;

import java.time.LocalDate;
import java.util.List;

public interface NewsProvider {
    List<NewsArticle> news(String symbol, LocalDate from, LocalDate to);
}
