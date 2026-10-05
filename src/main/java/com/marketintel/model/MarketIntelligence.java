package com.marketintel.model;

import java.util.List;

public record MarketIntelligence(
        String symbol,
        String marketRegime,
        String bias,
        String recommendation,
        int score,
        int technicalScore,
        int newsScore,
        int marketScore,
        int optionsScore,
        String strategy,
        String entryTrigger,
        String stopTrigger,
        String profitTarget,
        double maxRisk,
        double maxProfit,
        String riskLevel,
        List<String> reasons,
        List<String> warnings,
        String disclaimer) {
}