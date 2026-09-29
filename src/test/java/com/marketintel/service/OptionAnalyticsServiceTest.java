package com.marketintel.service;

import com.marketintel.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionAnalyticsServiceTest {
    @Test
    void detectsUnusualVolumeRelativeToOpenInterest() {
        OptionContract contract = new OptionContract(
                "NVDA-test", "NVDA", OptionType.CALL, LocalDate.now().plusDays(30),
                180, 4, 5, 4.5, 3000, 1000, 0.30,
                0.5, 0.02, -0.03, 0.10, 0.02, 100);

        OptionAnalyticsService service = new OptionAnalyticsService((symbol, expiration) ->
                new OptionChain(symbol, expiration, List.of(contract)));

        assertEquals(1, service.unusual("NVDA", contract.expiration()).size());
        assertEquals(3.0, service.unusual("NVDA", contract.expiration()).get(0).volumeOpenInterestRatio());
    }

    @Test
    void calculatesPositiveCallScenarioWhenUnderlyingRises() {
        LocalDate expiration = LocalDate.now().plusDays(30);
        OptionAnalyticsService service = new OptionAnalyticsService((symbol, date) ->
                new OptionChain(symbol, date, List.of()));

        OptionScenario result = service.scenario(new OptionScenarioRequest(
                "NVDA", OptionType.CALL, 180, expiration,
                180, 0.30, 200, 0.04, 100));

        assertTrue(result.theoreticalPrice() > 0);
        assertTrue(result.estimatedProfitLoss() > 0);
        assertTrue(result.delta() > 0);
        assertTrue(result.gamma() > 0);
    }
}
