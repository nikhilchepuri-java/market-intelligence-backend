package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OptionAlertService {
    private final OptionsProvider provider;
    private final AtomicLong ids=new AtomicLong(1);
    private final Map<Long,OptionAlertRule> rules=new ConcurrentHashMap<>();
    private final Map<Long,Instant> lastTriggered=new ConcurrentHashMap<>();
    public OptionAlertService(OptionsProvider provider){this.provider=provider;}
    public List<OptionAlertRule> list(String underlying){return rules.values().stream().filter(r->underlying==null||underlying.equalsIgnoreCase(r.underlying())).sorted(Comparator.comparingLong(OptionAlertRule::id)).toList();}
    public OptionAlertRule create(OptionAlertRule rule){long id=ids.getAndIncrement();OptionAlertRule saved=new OptionAlertRule(id,rule.underlying(),rule.symbol(),rule.type(),rule.threshold(),rule.enabled(),rule.cooldownSeconds());rules.put(id,saved);return saved;}
    public void delete(long id){rules.remove(id);lastTriggered.remove(id);}
    public List<OptionAlertEvent> evaluate(String underlying, java.time.LocalDate expiration){
        var contracts=provider.chain(underlying,expiration).contracts();
        List<OptionAlertEvent> events=new ArrayList<>();
        for(OptionAlertRule rule:list(underlying)){
            if(!rule.enabled()) continue;
            for(OptionContract c:contracts){
                if(rule.symbol()!=null&&!rule.symbol().isBlank()&&!rule.symbol().equalsIgnoreCase(c.symbol())) continue;
                double value=value(rule,c);
                if(!matches(rule,value)) continue;
                Instant now=Instant.now(),last=lastTriggered.get(rule.id());
                if(last!=null&&now.isBefore(last.plusSeconds(Math.max(0,rule.cooldownSeconds())))) continue;
                lastTriggered.put(rule.id(),now);
                events.add(new OptionAlertEvent(rule.id(),c.underlying(),c.symbol(),rule.type(),value,rule.threshold(),now,
                        c.symbol()+" "+rule.type()+" triggered at "+value));
            }
        }
        return events;
    }
    private double value(OptionAlertRule r,OptionContract c){return switch(r.type()){
        case VOLUME_OI_RATIO_ABOVE -> c.openInterest()==0?Double.POSITIVE_INFINITY:(double)c.volume()/c.openInterest();
        case IMPLIED_VOLATILITY_ABOVE,IMPLIED_VOLATILITY_BELOW -> c.impliedVolatility();
        case OPEN_INTEREST_ABOVE -> c.openInterest(); case VOLUME_ABOVE -> c.volume();
        case PREMIUM_ABOVE -> ((c.bid()+c.ask())/2)*c.volume()*c.contractSize();};}
    private boolean matches(OptionAlertRule r,double v){return switch(r.type()){
        case IMPLIED_VOLATILITY_BELOW -> v<=r.threshold(); default -> v>=r.threshold();};}
}
