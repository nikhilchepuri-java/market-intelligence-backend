package com.marketintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.NewsArticle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "market.provider", havingValue = "finnhub")
public class FinnhubNewsProvider implements NewsProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;

    public FinnhubNewsProvider(org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("market.finnhub-api-key", "");
    }

    @Override
    public List<NewsArticle> news(String symbol, LocalDate from, LocalDate to) {
        try {
            String url = "https://finnhub.io/api/v1/company-news?symbol=" + symbol.toUpperCase()
                    + "&from=" + from
                    + "&to=" + to
                    + "&token=" + apiKey;

            JsonNode root = mapper.readTree(client.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body());

            List<NewsArticle> result = new ArrayList<>();
            if (!root.isArray()) {
                return result;
            }

            for (JsonNode item : root) {
                result.add(new NewsArticle(
                        symbol.toUpperCase(),
                        item.path("id").asLong(),
                        Instant.ofEpochSecond(item.path("datetime").asLong()),
                        item.path("headline").asText(""),
                        item.path("source").asText(""),
                        item.path("summary").asText(""),
                        item.path("url").asText(""),
                        item.path("image").asText(""),
                        item.path("category").asText(""),
                        item.path("related").asText("")
                ));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("News request failed for " + symbol, e);
        }
    }
}
