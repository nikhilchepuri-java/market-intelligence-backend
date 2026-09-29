package com.marketintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.Candle;
import com.marketintel.model.Quote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name = "market.provider", havingValue = "finnhub")
public class FinnhubMarketDataProvider implements MarketDataProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;

    public FinnhubMarketDataProvider(org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("market.finnhub-api-key", "");
    }

    public Quote quote(String symbol) {
        try {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + apiKey;
            JsonNode n = mapper.readTree(client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
            double c = n.path("c").asDouble(), pc = n.path("pc").asDouble();
            double ch = c - pc;
            return new Quote(symbol.toUpperCase(), c, ch, pc == 0 ? 0 : ch / pc * 100, n.path("h").asDouble(), n.path("l").asDouble(), 0, Instant.now().toString());
        } catch (Exception e) {
            throw new IllegalStateException("Market data request failed for " + symbol, e);
        }
    }

    public List<Quote> quotes(List<String> symbols) {
        return symbols.stream().map(this::quote).toList();
    }

    @Override
    public List<Candle> candles(String symbol, String timeframe, Instant from, Instant to) {
        try {
            String resolution = switch (timeframe.toUpperCase()) {
                case "1D", "D", "DAY" -> "D";
                case "1H", "60" -> "60";
                case "30M", "30" -> "30";
                case "15M", "15" -> "15";
                case "5M", "5" -> "5";
                case "1M", "1" -> "1";
                default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
            };

            String url = "https://finnhub.io/api/v1/stock/candle?symbol=" + symbol.toUpperCase()
                    + "&resolution=" + resolution
                    + "&from=" + from.getEpochSecond()
                    + "&to=" + to.getEpochSecond()
                    + "&token=" + apiKey;

            JsonNode n = mapper.readTree(client.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body());

            if (!"ok".equalsIgnoreCase(n.path("s").asText())) {
                return List.of();
            }

            JsonNode opens = n.path("o"), highs = n.path("h"), lows = n.path("l");
            JsonNode closes = n.path("c"), volumes = n.path("v"), timestamps = n.path("t");
            List<Candle> result = new ArrayList<>();

            for (int i = 0; i < timestamps.size(); i++) {
                result.add(new Candle(
                        symbol.toUpperCase(),
                        timeframe.toUpperCase(),
                        Instant.ofEpochSecond(timestamps.get(i).asLong()),
                        opens.get(i).asDouble(),
                        highs.get(i).asDouble(),
                        lows.get(i).asDouble(),
                        closes.get(i).asDouble(),
                        volumes.get(i).asLong()
                ));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Historical candle request failed for " + symbol, e);
        }
    }
}