package com.marketintel.service;

import com.marketintel.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StrategyAnalyticsService {
    public StrategyScenario scenario(StrategyScenarioRequest request) {
        double netPremium = request.legs().stream().mapToDouble(this::signedPremium).sum();
        List<StrategyPayoffPoint> payoff = new ArrayList<>();
        int points = Math.max(2, request.gridPoints());
        double step = (request.gridMaxPrice() - request.gridMinPrice()) / (points - 1);
        for (int i = 0; i < points; i++) {
            double price = request.gridMinPrice() + step * i;
            payoff.add(new StrategyPayoffPoint(price, profitAtExpiry(price, request.legs(), netPremium)));
        }
        double maxProfit = payoff.stream().mapToDouble(StrategyPayoffPoint::profitLoss).max().orElse(0);
        double maxLoss = payoff.stream().mapToDouble(StrategyPayoffPoint::profitLoss).min().orElse(0);
        List<Double> breakevens = breakevens(payoff);
        Greeks g = greeks(request.underlyingPrice(), request.riskFreeRate(), request.legs());
        return new StrategyScenario(request.underlying(), request.underlyingPrice(), netPremium,
                maxProfit, maxLoss, breakevens, g.delta(), g.gamma(), g.theta(), g.vega(), g.rho(), payoff);
    }

    private double signedPremium(OptionLeg leg) {
        double cash = leg.premium() * leg.quantity() * leg.contractSize();
        return leg.side() == OptionLegSide.BUY ? -cash : cash;
    }

    private double profitAtExpiry(double spot, List<OptionLeg> legs, double netPremium) {
        double intrinsic = legs.stream().mapToDouble(l -> {
            double value = l.optionType() == OptionType.CALL
                    ? Math.max(spot - l.strike(), 0)
                    : Math.max(l.strike() - spot, 0);
            double signed = l.side() == OptionLegSide.BUY ? value : -value;
            return signed * l.quantity() * l.contractSize();
        }).sum();
        return intrinsic + netPremium;
    }

    private List<Double> breakevens(List<StrategyPayoffPoint> points) {
        List<Double> result = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            StrategyPayoffPoint a = points.get(i - 1), b = points.get(i);
            if ((a.profitLoss() <= 0 && b.profitLoss() >= 0) || (a.profitLoss() >= 0 && b.profitLoss() <= 0)) {
                if (a.profitLoss() == b.profitLoss()) result.add(a.underlyingPrice());
                else {
                    double fraction = -a.profitLoss() / (b.profitLoss() - a.profitLoss());
                    result.add(a.underlyingPrice() + fraction * (b.underlyingPrice() - a.underlyingPrice()));
                }
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    private Greeks greeks(double spot, double rate, List<OptionLeg> legs) {
        double d=0,g=0,t=0,v=0,r=0;
        for (OptionLeg leg : legs) {
            double years = Math.max(1.0 / 365, ChronoUnit.DAYS.between(LocalDate.now(), leg.expiration()) / 365.0);
            Greeks x = legGreeks(spot, leg.strike(), years, rate, leg.impliedVolatility(), leg.optionType());
            double sign = leg.side() == OptionLegSide.BUY ? 1 : -1;
            double scale = sign * leg.quantity() * leg.contractSize();
            d += x.delta()*scale; g += x.gamma()*scale; t += x.theta()*scale; v += x.vega()*scale; r += x.rho()*scale;
        }
        return new Greeks(d,g,t,v,r);
    }

    private Greeks legGreeks(double s,double k,double time,double rate,double sigma,OptionType type){
        double sqrt=Math.sqrt(time), d1=(Math.log(s/k)+(rate+sigma*sigma/2)*time)/(sigma*sqrt), d2=d1-sigma*sqrt;
        double pdf=Math.exp(-d1*d1/2)/Math.sqrt(2*Math.PI), gamma=pdf/(s*sigma*sqrt), vega=s*pdf*sqrt/100;
        double delta=type==OptionType.CALL?cdf(d1):cdf(d1)-1;
        double thetaAnnual=type==OptionType.CALL?-(s*pdf*sigma)/(2*sqrt)-rate*k*Math.exp(-rate*time)*cdf(d2):-((s*pdf*sigma)/(2*sqrt))+rate*k*Math.exp(-rate*time)*cdf(-d2);
        double theta=thetaAnnual/365;
        double rho=type==OptionType.CALL?k*time*Math.exp(-rate*time)*cdf(d2)/100:-k*time*Math.exp(-rate*time)*cdf(-d2)/100;
        return new Greeks(delta,gamma,theta,vega,rho);
    }
    private double cdf(double x){return 0.5*(1+erf(x/Math.sqrt(2)));}
    private double erf(double x){double sign=x<0?-1:1;x=Math.abs(x);double q=1/(1+0.3275911*x);double y = 1 - (((((1.061405429 * q - 1.453152027) * q) + 1.421413741) * q - 0.284496736) * q + 0.254829592) * q * Math.exp(-x * x);return sign*y;}
    private record Greeks(double delta,double gamma,double theta,double vega,double rho){}
}
