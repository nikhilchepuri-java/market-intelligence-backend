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
    private final String alphaVantageApiKey;

    public FinnhubMarketDataProvider(org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("market.finnhub-api-key", "");
        this.alphaVantageApiKey = env.getProperty("market.alpha-vantage-api-key", "");
    }

    @Override
    public Quote quote(String symbol) {
        String requestedSymbol = symbol.toUpperCase(Locale.ROOT);
        String providerSymbol = providerSymbol(requestedSymbol);
        ensureFinnhubSymbolSupported(requestedSymbol);
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
        List<Quote> result = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                result.add(quote(symbol));
            } catch (RuntimeException ignored) {
                // A provider entitlement for one symbol must not make the entire quote
                // dashboard unavailable. The caller can treat the symbol as unavailable.
            }
        }
        return result;
    }

    @Override
    public List<Candle> candles(String symbol, String timeframe, Instant from, Instant to) {
        String requestedSymbol = symbol.toUpperCase(Locale.ROOT);
        String providerSymbol = candleProviderSymbol(requestedSymbol);

        if (alphaVantageApiKey == null || alphaVantageApiKey.isBlank()) {
            throw new IllegalStateException("ALPHA_VANTAGE_API_KEY is not configured");
        }

        String tf = timeframe.toUpperCase(Locale.ROOT);
        String function;
        String interval = null;
        switch (tf) {
            case "1D", "D", "DAY" -> function = "TIME_SERIES_DAILY";
            case "1M", "1", "5M", "5", "15M", "15", "30M", "30", "1H", "60" -> {
                function = "TIME_SERIES_INTRADAY";
                interval = switch (tf) {
                    case "1M", "1" -> "1min";
                    case "5M", "5" -> "5min";
                    case "15M", "15" -> "15min";
                    case "30M", "30" -> "30min";
                    case "1H", "60" -> "60min";
                    default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
                };
            }
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }

        String query = "/query?function=" + function
                + "&symbol=" + encode(providerSymbol)
                + (interval == null ? "" : "&interval=" + interval + "&outputsize=compact")
                + "&apikey=" + encode(alphaVantageApiKey);
        JsonNode n = alphaVantageRequest(query);
        String seriesName = interval == null ? "Time Series (Daily)" : "Time Series (" + interval + ")";
        JsonNode series = n.path(seriesName);
        if (!series.isObject()) {
            throw new IllegalStateException("Alpha Vantage returned no candle series for " + providerSymbol
                    + ": " + alphaVantageMessage(n));
        }

        List<Candle> result = new ArrayList<>();
        Iterator<String> fields = series.fieldNames();
        while (fields.hasNext()) {
            String timestampText = fields.next();
            Instant timestamp;
            try {
                timestamp = interval == null
                        ? java.time.LocalDate.parse(timestampText).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                        : java.time.LocalDateTime.parse(timestampText,
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            .toInstant(java.time.ZoneOffset.UTC);
            } catch (java.time.format.DateTimeParseException e) {
                continue;
            }
            if (timestamp.isBefore(from) || timestamp.isAfter(to)) continue;

            JsonNode point = series.path(timestampText);
            result.add(new Candle(
                    requestedSymbol,
                    tf,
                    timestamp,
                    point.path("1. open").asDouble(Double.NaN),
                    point.path("2. high").asDouble(Double.NaN),
                    point.path("3. low").asDouble(Double.NaN),
                    point.path("4. close").asDouble(Double.NaN),
                    point.path("5. volume").asLong(0)
            ));
        }
        result.sort(Comparator.comparing(Candle::timestamp));
        return result;
    }

    private JsonNode alphaVantageRequest(String pathAndQuery) {
        String url = "https://www.alphavantage.co" + pathAndQuery;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Alpha Vantage HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            if (root == null || root.isMissingNode()) {
                throw new IllegalStateException("Alpha Vantage returned an empty JSON response");
            }
            if (root.has("Error Message")) {
                throw new IllegalStateException("Alpha Vantage API error: " + alphaVantageMessage(root));
            }
            if (root.has("Note")) {
                throw new IllegalStateException("Alpha Vantage rate-limit/provider notice: " + alphaVantageMessage(root));
            }
            if (root.has("Information")) {
                throw new IllegalStateException("Alpha Vantage provider information: " + alphaVantageMessage(root));
            }
            return root;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Alpha Vantage request interrupted", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Alpha Vantage request failed: " + e.getMessage(), e);
        }
    }

    private String alphaVantageMessage(JsonNode root) {
        if (root.has("Error Message")) return root.path("Error Message").asText();
        if (root.has("Note")) return root.path("Note").asText();
        if (root.has("Information")) return root.path("Information").asText();
        return "unexpected response";
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

    private void ensureFinnhubSymbolSupported(String symbol) {
        if ("NDX".equals(symbol)) {
            throw new IllegalStateException(
                    "Finnhub NDX index/CFD data is not enabled for this API key. " +
                    "Use a supported US equity/ETF symbol such as QQQ, or enable the required Finnhub entitlement."
            );
        }
    }

    private String providerSymbol(String symbol) {
        return symbol;
    }

    private String candleProviderSymbol(String symbol) {
        return "NDX".equals(symbol) ? "QQQ" : symbol;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
