package com.marketintel.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinnhubMarketDataProviderTest {

    private FinnhubMarketDataProvider provider() {
        return new FinnhubMarketDataProvider(
                new MockEnvironment().withProperty("market.finnhub-api-key", "test-key")
        );
    }

    @Test
    void rejectsNdxQuoteWithoutCallingProvider() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider().quote("NDX")
        );

        assertTrue(error.getMessage().contains("NDX"));
        assertTrue(error.getMessage().contains("entitlement"));
    }

    @Test
    void rejectsNdxCandlesWithoutCallingProvider() {
        Instant to = Instant.now();
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider().candles("NDX", "1D", to.minusSeconds(86400), to)
        );

        assertTrue(error.getMessage().contains("NDX"));
        assertTrue(error.getMessage().contains("entitlement"));
    }
}
