package com.marketintel.model;
import java.util.List;
public record BacktestResult(String symbol,String strategy,double initialCapital,double finalCapital,double totalReturnPercent,double maxDrawdownPercent,double winRate,int trades,List<BacktestTrade> tradesList) {}