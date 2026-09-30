package com.marketintel.model;
import java.time.Instant;
public record FeaturePoint(String symbol, Instant timestamp, double close, double return1, double sma20, double sma50, double ema21, double rsi14, double atr14, double relativeVolume20) {}