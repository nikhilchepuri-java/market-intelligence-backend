package com.marketintel.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record StrategyScenarioRequest(
        @NotBlank String underlying,
        @Positive double underlyingPrice,
        @PositiveOrZero double riskFreeRate,
        @NotEmpty List<@Valid OptionLeg> legs,
        @PositiveOrZero double gridMinPrice,
        @Positive double gridMaxPrice,
        @Positive int gridPoints) {
}
