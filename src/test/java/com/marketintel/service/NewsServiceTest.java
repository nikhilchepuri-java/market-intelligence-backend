package com.marketintel.service;

import com.marketintel.model.NewsArticle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceTest {

    @Test
    void delegatesNewsQueryToProvider() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 7);
        NewsArticle article = new NewsArticle(
                "NVDA", 1L, Instant.parse("2026-09-05T12:00:00Z"),
                "NVIDIA launches new platform", "Example", "Summary",
                "https://example.com/news", "", "company", "NVDA");

        NewsProvider provider = (symbol, start, end) -> {
            assertEquals("NVDA", symbol);
            assertEquals(from, start);
            assertEquals(to, end);
            return List.of(article);
        };

        NewsService service = new NewsService(provider);

        assertEquals(List.of(article), service.news("NVDA", from, to));
    }

    @Test
    void rejectsReversedDateRange() {
        NewsService service = new NewsService((symbol, from, to) -> List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.news("NVDA", LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 7)));
    }
}
