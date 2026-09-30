package com.marketintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.NewsArticle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@ConditionalOnProperty(name = "market.provider", havingValue = "finnhub")
public class FinnhubNewsProvider implements NewsProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String apiKey;

    public FinnhubNewsProvider(org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("market.finnhub-api-key", "");
    }

    @Override
    public List<NewsArticle> news(String symbol, LocalDate from, LocalDate to) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("FINNHUB_API_KEY is not configured");
        }

        String requestedSymbol = symbol.toUpperCase(Locale.ROOT);
        String path;
        if ("NDX".equals(requestedSymbol)) {
            path = "/news?category=general";
        } else {
            path = "/company-news?symbol=" + encode(requestedSymbol)
                    + "&from=" + from + "&to=" + to;
        }

        JsonNode root = request(path);
        if (!root.isArray()) {
            throw new IllegalStateException("Finnhub returned an invalid news payload for " + requestedSymbol);
        }

        List<NewsArticle> result = new ArrayList<>();
        for (JsonNode item : root) {
            long publishedEpoch = item.path("datetime").asLong(0);
            result.add(new NewsArticle(
                    requestedSymbol,
                    item.path("id").asLong(),
                    publishedEpoch > 0 ? Instant.ofEpochSecond(publishedEpoch) : Instant.now(),
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
    }

    private JsonNode request(String pathAndQuery) {
        String separator = pathAndQuery.contains("?") ? "&" : "?";
        String url = "https://finnhub.io/api/v1" + pathAndQuery + separator + "token=" + encode(apiKey);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = response.body() == null ? "" : response.body().trim();
                if (body.length() > 240) body = body.substring(0, 240);
                throw new IllegalStateException("Finnhub HTTP " + response.statusCode()
                        + (body.isBlank() ? "" : ": " + body));
            }
            JsonNode root = mapper.readTree(response.body());
            if (root == null || root.isMissingNode()) {
                throw new IllegalStateException("Finnhub returned an empty JSON response");
            }
            if (root.has("error") && !root.path("error").asText("").isBlank()) {
                throw new IllegalStateException("Finnhub API error: " + root.path("error").asText());
            }
            return root;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Finnhub request interrupted", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Finnhub request failed: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
