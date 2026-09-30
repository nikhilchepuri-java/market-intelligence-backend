package com.marketintel.controller;

import com.marketintel.model.*;
import com.marketintel.service.IvAnalyticsService;
import com.marketintel.service.OptionFlowAnalyticsService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;

@RestController
@RequestMapping("/api/options")
@CrossOrigin(origins="http://localhost:4200")
public class OptionAnalyticsController {
    private final IvAnalyticsService iv;
    private final OptionFlowAnalyticsService flow;
    public OptionAnalyticsController(IvAnalyticsService iv,OptionFlowAnalyticsService flow){this.iv=iv;this.flow=flow;}
    @GetMapping("/{symbol}/iv-surface")
    public IvSurface surface(@PathVariable String symbol,@RequestParam @Positive double spot,@RequestParam String expirations){
        var dates=Arrays.stream(expirations.split(",")).map(String::trim).filter(s->!s.isBlank()).map(LocalDate::parse).toList();
        return iv.surface(symbol,spot,dates);
    }
    @GetMapping("/{symbol}/flow")
    public OptionFlowAnalytics flow(@PathVariable String symbol,@RequestParam LocalDate expiration){return this.flow.analyze(symbol,expiration);}
}
