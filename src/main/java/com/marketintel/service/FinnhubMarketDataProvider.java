package com.marketintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
