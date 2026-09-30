package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class IvAnalyticsService {
    private final OptionsProvider provider;
    public IvAnalyticsService(OptionsProvider provider){this.provider=provider;}

    public IvSurface surface(String underlying,double spot,List<LocalDate> expirations){
        List<IvSurfacePoint> points=new ArrayList<>();
        List<IvSkew> skews=new ArrayList<>();
        for(LocalDate expiration: expirations){
            OptionChain chain=provider.chain(underlying,expiration);
            List<OptionContract> calls=chain.contracts().stream().filter(c->c.optionType()==OptionType.CALL && c.impliedVolatility()>0).toList();
            List<OptionContract> puts=chain.contracts().stream().filter(c->c.optionType()==OptionType.PUT && c.impliedVolatility()>0).toList();
            for(OptionContract c: chain.contracts()) if(c.impliedVolatility()>0)
                points.add(new IvSurfacePoint(expiration,c.optionType(),c.strike(),c.strike()/spot,c.impliedVolatility()));
            double callAvg=calls.stream().mapToDouble(OptionContract::impliedVolatility).average().orElse(0);
            double putAvg=puts.stream().mapToDouble(OptionContract::impliedVolatility).average().orElse(0);
            skews.add(new IvSkew(expiration,callAvg,putAvg,putAvg-callAvg));
        }
        return new IvSurface(spot,points,skews);
    }
}
