package com.marketintel.model;
import java.time.Instant;
public record BacktestTrade(Instant entryTime,Instant exitTime,double entryPrice,double exitPrice,double quantity,double profitLoss,double returnPercent,String reason) {}