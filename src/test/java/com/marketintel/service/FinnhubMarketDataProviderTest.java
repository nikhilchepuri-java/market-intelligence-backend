package com.marketintel.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void mapsNdxCandlesToQqqProxy() {
        FinnhubMarketDataProvider provider = provider();

        assertEquals("QQQ", provider.candleProviderSymbol("NDX"));
        assertEquals("SPY", provider.candleProviderSymbol("SPY"));
    }
}
