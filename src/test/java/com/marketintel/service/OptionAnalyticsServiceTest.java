package com.marketintel.service;

import com.marketintel.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionAnalyticsServiceTest {

    @Test
    void detectsUnusualVolumeRelativeToOpenInterest() {
        LocalDate expiration = LocalDate.now().plusDays(30);

        OptionContract contract = new OptionContract(
                "NVDA-test", "NVDA", OptionType.CALL, expiration,
                180, 4, 5, 4.5, 3000, 1000, 0.30,
                0.5, 0.02, -0.03, 0.10, 0.02, 100);

        OptionsProvider provider = new OptionsProvider() {
            @Override
            public List<LocalDate> expirations(String underlying) {
                return List.of(expiration);
            }

            @Override
            public OptionChain chain(String underlying, LocalDate requestedExpiration) {
                return new OptionChain(
                        underlying,
                        requestedExpiration,
                        List.of(contract)
                );
            }
        };

        OptionAnalyticsService service =
                new OptionAnalyticsService(provider);

        assertEquals(
                1,
                service.unusual("NVDA", expiration).size()
        );

        assertEquals(
                3.0,
                service.unusual("NVDA", expiration)
                        .get(0)
                        .volumeOpenInterestRatio()
        );
    }

    @Test
    void calculatesPositiveCallScenarioWhenUnderlyingRises() {
        LocalDate expiration = LocalDate.now().plusDays(30);

        OptionsProvider provider = new OptionsProvider() {
            @Override
            public List<LocalDate> expirations(String underlying) {
                return List.of(expiration);
            }

            @Override
            public OptionChain chain(String underlying, LocalDate requestedExpiration) {
                return new OptionChain(
                        underlying,
                        requestedExpiration,
                        List.of()
                );
            }
        };

        OptionAnalyticsService service =
                new OptionAnalyticsService(provider);

        OptionScenario result = service.scenario(new OptionScenarioRequest(
                "NVDA",
                OptionType.CALL,
                180,
                expiration,
                180,
                0.30,
                200,
                0.04,
                100
        ));

        assertTrue(result.theoreticalPrice() > 0);
        assertTrue(result.estimatedProfitLoss() > 0);
        assertTrue(result.delta() > 0);
        assertTrue(result.gamma() > 0);
    }
}