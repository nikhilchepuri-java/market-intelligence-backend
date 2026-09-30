package com.marketintel.model;

import java.time.Instant;

public record OptionAlertEvent(
        long ruleId,
        String underlying,
        String symbol,
        OptionAlertType type,
        double value,
        double threshold,
        Instant triggeredAt,
        String message) {}
