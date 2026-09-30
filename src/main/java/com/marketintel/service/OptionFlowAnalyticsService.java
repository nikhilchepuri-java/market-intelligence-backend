package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

@Service
public class OptionFlowAnalyticsService {
    private final OptionsProvider provider;
    public OptionFlowAnalyticsService(OptionsProvider provider){this.provider=provider;}
    public OptionFlowAnalytics analyze(String underlying, java.time.LocalDate expiration){
        var contracts=provider.chain(underlying,expiration).contracts();
        long callOi=contracts.stream().filter(c->c.optionType()==OptionType.CALL).mapToLong(OptionContract::openInterest).sum();
        long putOi=contracts.stream().filter(c->c.optionType()==OptionType.PUT).mapToLong(OptionContract::openInterest).sum();
        long callVol=contracts.stream().filter(c->c.optionType()==OptionType.CALL).mapToLong(OptionContract::volume).sum();
        long putVol=contracts.stream().filter(c->c.optionType()==OptionType.PUT).mapToLong(OptionContract::volume).sum();
        long totalOi=callOi+putOi,totalVol=callVol+putVol;
        return new OptionFlowAnalytics(underlying,callOi,putOi,callOi==0?0:(double)putOi/callOi,callVol,putVol,callVol==0?0:(double)putVol/callVol,totalOi,totalVol,totalVol==0?0:(double)callVol/totalVol,totalVol==0?0:(double)putVol/totalVol);
    }
}
