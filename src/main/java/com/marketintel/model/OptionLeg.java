package com.marketintel.model;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record OptionLeg(
        OptionType optionType,
        OptionLegSide side,
        double strike,
        LocalDate expiration,
        @Positive int quantity,
        @PositiveOrZero double premium,
        @Positive double impliedVolatility,
        @PositiveOrZero double contractSize) {
}
