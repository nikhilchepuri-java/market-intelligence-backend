package com.marketintel.model;
import java.time.Instant;
public record PatternSignal(String symbol,Instant timestamp,String pattern,String direction,double confidence,String description) {}