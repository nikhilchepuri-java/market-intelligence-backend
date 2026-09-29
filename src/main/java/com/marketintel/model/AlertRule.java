package com.marketintel.model;

import java.time.Instant;

public record AlertRule(
        Long id,
        String symbol,
        AlertRuleType type,
        double threshold,
        boolean enabled,
        long cooldownSeconds,
        Instant lastTriggeredAt) {
}
