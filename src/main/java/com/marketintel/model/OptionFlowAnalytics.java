package com.marketintel.model;

public record OptionFlowAnalytics(
        String underlying,
        long callOpenInterest,
        long putOpenInterest,
        double putCallOpenInterestRatio,
        long callVolume,
        long putVolume,
        double putCallVolumeRatio,
        long totalOpenInterest,
        long totalVolume,
        double callVolumeShare,
        double putVolumeShare) {}
