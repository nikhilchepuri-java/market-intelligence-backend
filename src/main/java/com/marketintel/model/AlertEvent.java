package com.marketintel.model;

import java.time.Instant;

public record AlertEvent(
        Long ruleId,
        String symbol,
        AlertRuleType type,
        double value,
        double threshold,
        Instant triggeredAt,
        String message) {
}
