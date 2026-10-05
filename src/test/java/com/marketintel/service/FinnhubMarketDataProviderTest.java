package com.marketintel.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void mapsNdxCandlesToQqqProxyBeforeCallingProvider() {
        Instant to = Instant.now();
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider().candles("NDX", "1D", to.minusSeconds(86400), to)
        );

        // No Alpha Vantage key is configured in this unit test, so reaching this
        // validation proves NDX was not rejected as an unsupported index.
        assertEquals("ALPHA_VANTAGE_API_KEY is not configured", error.getMessage());

        try {
            Method mapper = FinnhubMarketDataProvider.class
                    .getDeclaredMethod("candleProviderSymbol", String.class);
            mapper.setAccessible(true);
            assertEquals("QQQ", mapper.invoke(provider(), "NDX"));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to verify NDX candle proxy mapping", e);
        }
    }
}
