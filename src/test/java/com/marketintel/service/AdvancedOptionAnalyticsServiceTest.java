package com.marketintel.service;

import com.marketintel.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedOptionAnalyticsServiceTest {
    private final LocalDate expiration = LocalDate.now().plusDays(30);

    @Test
    void calculatesLongCallPayoffAndBreakeven() {
        OptionLeg leg = new OptionLeg(OptionType.CALL, OptionLegSide.BUY, 180, expiration, 1, 5, .30, 100);
        StrategyScenario result = new StrategyAnalyticsService().scenario(new StrategyScenarioRequest(
                "NVDA", 180, .04, List.of(leg), 150, 220, 71));
        assertEquals(-500, result.netPremium(), .001);
        assertTrue(result.maxProfit() > 0);
        assertEquals(185, result.breakevenPrices().get(0), .5);
        assertTrue(result.delta() > 0);
    }

    @Test
    void computesPutCallFlowRatios() {
        OptionContract call = contract("C", OptionType.CALL, 200, 1000, 2000, .25);
        OptionContract put = contract("P", OptionType.PUT, 180, 3000, 1000, .30);
        OptionsProvider provider = new OptionsProvider() {
            public List<LocalDate> expirations(String u){return List.of(expiration);}
            public OptionChain chain(String u, LocalDate e){return new OptionChain(u,e,List.of(call,put));}
        };
        OptionFlowAnalytics x = new OptionFlowAnalyticsService(provider).analyze("NVDA", expiration);
        assertEquals(2000, x.callOpenInterest());
        assertEquals(1000, x.putOpenInterest());
        assertEquals(1.5, x.putCallVolumeRatio(), .001);
        assertEquals(4000, x.totalVolume());
    }

    @Test
    void buildsIvSurfaceAndSkew() {
        OptionContract call = contract("C", OptionType.CALL, 200, 100, 100, .25);
        OptionContract put = contract("P", OptionType.PUT, 180, 100, 100, .35);
        OptionsProvider provider = new OptionsProvider() {
            public List<LocalDate> expirations(String u){return List.of(expiration);}
            public OptionChain chain(String u, LocalDate e){return new OptionChain(u,e,List.of(call,put));}
        };
        IvSurface x = new IvAnalyticsService(provider).surface("NVDA",190,List.of(expiration));
        assertEquals(2,x.points().size());
        assertEquals(.10,x.skews().get(0).putCallSkew(),.001);
    }

    @Test
    void triggersOptionAlertWhenRatioExceedsThreshold() {
        OptionContract c = contract("C", OptionType.CALL, 200, 2500, 1000, .25);
        OptionsProvider provider = new OptionsProvider() {
            public List<LocalDate> expirations(String u){return List.of(expiration);}
            public OptionChain chain(String u, LocalDate e){return new OptionChain(u,e,List.of(c));}
        };
        OptionAlertService alerts = new OptionAlertService(provider);
        alerts.create(new OptionAlertRule(0,"NVDA","C",OptionAlertType.VOLUME_OI_RATIO_ABOVE,2,true,0));
        assertEquals(1,alerts.evaluate("NVDA",expiration).size());
    }

    private OptionContract contract(String symbol, OptionType type, double strike, long volume, long oi, double iv){
        return new OptionContract(symbol,"NVDA",type,expiration,strike,4,5,4.5,volume,oi,iv,.5,.02,-.03,.1,.02,100);
    }
}
