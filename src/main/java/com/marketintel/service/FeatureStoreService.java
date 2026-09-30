package com.marketintel.service;

import com.marketintel.model.*;
import com.marketintel.repository.HistoricalFeatureRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class FeatureStoreService {
 private final MarketDataProvider provider; private final HistoricalFeatureRepository repo;
 public FeatureStoreService(MarketDataProvider provider,HistoricalFeatureRepository repo){this.provider=provider;this.repo=repo;}
 public List<FeaturePoint> buildAndStore(String symbol,Instant from,Instant to){
  String s=symbol.toUpperCase(Locale.ROOT); var candles=new ArrayList<>(provider.candles(s,"1D",from,to)); candles.sort(Comparator.comparing(Candle::timestamp));
  List<HistoricalFeature> saved=new ArrayList<>();
  for(int i=0;i<candles.size();i++){var c=candles.get(i); double prev=i==0?c.close():candles.get(i-1).close(); double ret=prev==0?0:(c.close()/prev-1);
   double sma20=avgClose(candles,i,20),sma50=avgClose(candles,i,50),ema21=ema(candles,i,21),rsi=rsi(candles,i,14),atr=atr(candles,i,14),rv=relativeVolume(candles,i,20);
   saved.add(new HistoricalFeature(s,c.timestamp(),c.open(),c.high(),c.low(),c.close(),c.volume(),ret,sma20,sma50,ema21,rsi,atr,rv));
  }
  repo.deleteAll(repo.findBySymbolAndTimestampBetweenOrderByTimestampAsc(s,from,to)); repo.saveAll(saved);
  return saved.stream().map(x->new FeaturePoint(x.getSymbol(),x.getTimestamp(),x.getClose(),x.getReturn1(),x.getSma20(),x.getSma50(),x.getEma21(),x.getRsi14(),x.getAtr14(),x.getRelativeVolume20())).toList();
 }
 public List<FeaturePoint> load(String symbol,Instant from,Instant to){return repo.findBySymbolAndTimestampBetweenOrderByTimestampAsc(symbol.toUpperCase(Locale.ROOT),from,to).stream().map(x->new FeaturePoint(x.getSymbol(),x.getTimestamp(),x.getClose(),x.getReturn1(),x.getSma20(),x.getSma50(),x.getEma21(),x.getRsi14(),x.getAtr14(),x.getRelativeVolume20())).toList();}
 private double avgClose(List<Candle> x,int i,int p){int a=Math.max(0,i-p+1);return x.subList(a,i+1).stream().mapToDouble(Candle::close).average().orElse(0);}
 private double ema(List<Candle>x,int i,int p){int a=Math.max(0,i-p+1);double e=x.get(a).close(),alpha=2.0/(p+1);for(int j=a+1;j<=i;j++)e=alpha*x.get(j).close()+(1-alpha)*e;return e;}
 private double rsi(List<Candle>x,int i,int p){if(i==0)return 50;int a=Math.max(1,i-p+1);double g=0,l=0;for(int j=a;j<=i;j++){double d=x.get(j).close()-x.get(j-1).close();if(d>0)g+=d;else l-=d;}if(l==0)return g>0?100:50;return 100-100/(1+g/l);}
 private double atr(List<Candle>x,int i,int p){if(i==0)return 0;int a=Math.max(1,i-p+1);double sum=0;for(int j=a;j<=i;j++){var c=x.get(j);var prev=x.get(j-1);sum+=Math.max(c.high()-c.low(),Math.max(Math.abs(c.high()-prev.close()),Math.abs(c.low()-prev.close())));}return sum/(i-a+1);}
 private double relativeVolume(List<Candle>x,int i,int p){int a=Math.max(0,i-p+1);double avg=x.subList(a,i+1).stream().mapToLong(Candle::volume).average().orElse(0);return avg==0?0:x.get(i).volume()/avg;}
}