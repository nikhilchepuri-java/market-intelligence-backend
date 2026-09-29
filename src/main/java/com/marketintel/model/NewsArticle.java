package com.marketintel.model;

import java.time.Instant;

public record NewsArticle(
        String symbol,
        long id,
        Instant publishedAt,
        String headline,
        String source,
        String summary,
        String url,
        String imageUrl,
        String category,
        String related) {
}
