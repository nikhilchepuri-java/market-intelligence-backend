package com.marketintel.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AlertRuleRequest(
        @NotBlank String symbol,
        @NotNull AlertRuleType type,
        double threshold,
        boolean enabled,
        @PositiveOrZero Long cooldownSeconds) {
}
