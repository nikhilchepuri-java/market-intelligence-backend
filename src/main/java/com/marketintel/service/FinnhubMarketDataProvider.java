package com.marketintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.Candle;
import com.marketintel.model.Quote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name = "market.provider", havingValue = "finnhub")
public class FinnhubMarketDataProvider implements MarketDataProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String apiKey;

    public FinnhubMarketDataProvider(org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("market.finnhub-api-key", "");
    }

    @Override
    public Quote quote(String symbol) {
        String requestedSymbol = symbol.toUpperCase(Locale.ROOT);
        String providerSymbol = providerSymbol(requestedSymbol);
        JsonNode n = request("/quote?symbol=" + encode(providerSymbol));

        double c = n.path("c").asDouble(Double.NaN);
        double pc = n.path("pc").asDouble(Double.NaN);
        double h = n.path("h").asDouble(Double.NaN);
        double l = n.path("l").asDouble(Double.NaN);

        if (!Double.isFinite(c) || c <= 0 || !Double.isFinite(pc) || pc <= 0) {
            throw new IllegalStateException("Finnhub returned no usable quote for " + requestedSymbol
                    + " (provider symbol " + providerSymbol + ")");
        }

        double change = n.path("d").asDouble(c - pc);
        double changePercent = n.path("dp").asDouble(change / pc * 100.0);
        long quoteEpoch = n.path("t").asLong(0);

        return new Quote(
                requestedSymbol,
                c,
                change,
                changePercent,
                Double.isFinite(h) ? h : c,
                Double.isFinite(l) ? l : c,
                0,
                quoteEpoch > 0 ? Instant.ofEpochSecond(quoteEpoch).toString() : Instant.now().toString()
        );
    }

    @Override
    public List<Quote> quotes(List<String> symbols) {
        return symbols.stream().map(this::quote).toList();
    }

    @Override
    public List<Candle> candles(String symbol, String timeframe, Instant from, Instant to) {
        String requestedSymbol = symbol.toUpperCase(Locale.ROOT);
        String providerSymbol = providerSymbol(requestedSymbol);

        String resolution = switch (timeframe.toUpperCase(Locale.ROOT)) {
            case "1D", "D", "DAY" -> "D";
            case "1H", "60" -> "60";
            case "30M", "30" -> "30";
            case "15M", "15" -> "15";
            case "5M", "5" -> "5";
            case "1M", "1" -> "1";
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };

        JsonNode n = request("/stock/candle?symbol=" + encode(providerSymbol)
                + "&resolution=" + resolution
                + "&from=" + from.getEpochSecond()
                + "&to=" + to.getEpochSecond());

        String status = n.path("s").asText("");
        if ("no_data".equalsIgnoreCase(status)) {
            return List.of();
        }
        if (!"ok".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Finnhub returned candle status '" + status
                    + "' for " + requestedSymbol + " (provider symbol " + providerSymbol + ")");
        }

        JsonNode opens = n.path("o"), highs = n.path("h"), lows = n.path("l");
        JsonNode closes = n.path("c"), volumes = n.path("v"), timestamps = n.path("t");
        if (!timestamps.isArray() || !opens.isArray() || !highs.isArray()
                || !lows.isArray() || !closes.isArray() || !volumes.isArray()) {
            throw new IllegalStateException("Finnhub returned an invalid candle payload for " + requestedSymbol);
        }

        int count = timestamps.size();
        if (opens.size() != count || highs.size() != count || lows.size() != count
                || closes.size() != count || volumes.size() != count) {
            throw new IllegalStateException("Finnhub returned mismatched candle arrays for " + requestedSymbol);
        }

        List<Candle> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new Candle(
                    requestedSymbol,
                    timeframe.toUpperCase(Locale.ROOT),
                    Instant.ofEpochSecond(timestamps.get(i).asLong()),
                    opens.get(i).asDouble(),
                    highs.get(i).asDouble(),
                    lows.get(i).asDouble(),
                    closes.get(i).asDouble(),
                    volumes.get(i).asLong()
            ));
        }
        return result;
    }

    private JsonNode request(String pathAndQuery) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("FINNHUB_API_KEY is not configured");
        }

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

    private String providerSymbol(String symbol) {
        return switch (symbol) {
            case "NDX" -> "^NDX";
            default -> symbol;
        };
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
