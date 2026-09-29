package com.marketintel.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record OptionScenarioRequest(
        @NotBlank String underlying,
        @NotNull OptionType optionType,
        @Positive double strike,
        @NotNull LocalDate expiration,
        @Positive double underlyingPrice,
        @Positive double impliedVolatility,
        @Positive double targetUnderlyingPrice,
        @Positive double riskFreeRate,
        @Positive double contractSize) {
}
