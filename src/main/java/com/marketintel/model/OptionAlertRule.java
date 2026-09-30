package com.marketintel.model;

public record OptionAlertRule(
        long id,
        String underlying,
        String symbol,
        OptionAlertType type,
        double threshold,
        boolean enabled,
        int cooldownSeconds) {}
