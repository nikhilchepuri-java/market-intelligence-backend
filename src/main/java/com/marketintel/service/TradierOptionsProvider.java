package com.marketintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "market.options-provider", havingValue = "tradier")
public class TradierOptionsProvider implements OptionsProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String token;
    private final String baseUrl;

    public TradierOptionsProvider(Environment env) {
        this.token = env.getProperty("market.tradier-token", "");
        this.baseUrl = env.getProperty("market.tradier-base-url", "https://api.tradier.com");
    }

    @Override
    public List<LocalDate> expirations(String underlying) {
        try {
            String url = baseUrl + "/v1/markets/options/expirations?symbol=" + underlying.toUpperCase() + "&includeAllRoots=true";
            JsonNode root = request(url);
            List<LocalDate> result = new ArrayList<>();
            JsonNode dates = root.path("expirations").path("date");
            if (dates.isArray()) for (JsonNode date : dates) result.add(LocalDate.parse(date.asText()));
            else if (!dates.isMissingNode()) result.add(LocalDate.parse(dates.asText()));
            return result;
        } catch (Exception e) {
            String detail = e.getMessage();
            String suffix = detail == null || detail.isBlank() ? "" : ": " + detail;
            throw new IllegalStateException("Options expiration request failed for " + underlying + suffix, e);
        }
    }

    @Override
    public OptionChain chain(String underlying, LocalDate expiration) {
        try {
            String url = baseUrl + "/v1/markets/options/chains?symbol=" + underlying.toUpperCase()
                    + "&expiration=" + expiration + "&greeks=true";
            JsonNode root = request(url);
            JsonNode options = root.path("options").path("option");
            List<OptionContract> result = new ArrayList<>();
            if (!options.isArray() && options.isObject()) options = mapper.createArrayNode().add(options);
            for (JsonNode n : options) {
                JsonNode g = n.path("greeks");
                result.add(new OptionContract(
                        n.path("symbol").asText(""),
                        underlying.toUpperCase(),
                        "put".equalsIgnoreCase(n.path("option_type").asText()) ? OptionType.PUT : OptionType.CALL,
                        LocalDate.parse(n.path("expiration_date").asText(expiration.toString())),
                        n.path("strike").asDouble(),
                        n.path("bid").asDouble(),
                        n.path("ask").asDouble(),
                        n.path("last").asDouble(),
                        n.path("volume").asLong(),
                        n.path("open_interest").asLong(),
                        g.path("mid_iv").asDouble(g.path("smv_vol").asDouble()),
                        g.path("delta").asDouble(),
                        g.path("gamma").asDouble(),
                        g.path("theta").asDouble(),
                        g.path("vega").asDouble(),
                        g.path("rho").asDouble(),
                        n.path("contract_size").asDouble(100)
                ));
            }
            return new OptionChain(underlying.toUpperCase(), expiration, result);
        } catch (Exception e) {
            throw new IllegalStateException("Options chain request failed for " + underlying + " " + expiration, e);
        }
    }

    private JsonNode request(String url) throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Tradier token is not configured");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Tradier returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }
}
