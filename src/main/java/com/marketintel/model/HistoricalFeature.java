package com.marketintel.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="historical_features", indexes=@Index(name="idx_feature_symbol_time", columnList="symbol,timestamp", unique=true))
public class HistoricalFeature {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String symbol;
    @Column(nullable=false) private Instant timestamp;
    private double open, high, low, close;
    private long volume;
    private double return1, sma20, sma50, ema21, rsi14, atr14, relativeVolume20;
    protected HistoricalFeature() {}
    public HistoricalFeature(String symbol, Instant timestamp, double open, double high, double low, double close, long volume,
                             double return1, double sma20, double sma50, double ema21, double rsi14, double atr14, double relativeVolume20) {
        this.symbol=symbol; this.timestamp=timestamp; this.open=open; this.high=high; this.low=low; this.close=close; this.volume=volume;
        this.return1=return1; this.sma20=sma20; this.sma50=sma50; this.ema21=ema21; this.rsi14=rsi14; this.atr14=atr14; this.relativeVolume20=relativeVolume20;
    }
    public Long getId(){return id;} public String getSymbol(){return symbol;} public Instant getTimestamp(){return timestamp;}
    public double getOpen(){return open;} public double getHigh(){return high;} public double getLow(){return low;} public double getClose(){return close;}
    public long getVolume(){return volume;} public double getReturn1(){return return1;} public double getSma20(){return sma20;} public double getSma50(){return sma50;}
    public double getEma21(){return ema21;} public double getRsi14(){return rsi14;} public double getAtr14(){return atr14;} public double getRelativeVolume20(){return relativeVolume20;}
}