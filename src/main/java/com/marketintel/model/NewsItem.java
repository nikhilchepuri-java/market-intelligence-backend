package com.marketintel.model;

public record NewsItem(String symbol, String headline, String source, String url, String publishedAt,
                       String sentiment) {
}
