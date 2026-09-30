package com.marketintel.controller;

import com.marketintel.model.StrategyScenario;
import com.marketintel.model.StrategyScenarioRequest;
import com.marketintel.service.StrategyAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/options/strategies")
@CrossOrigin(origins="http://localhost:4200")
public class OptionStrategyController {
    private final StrategyAnalyticsService service;
    public OptionStrategyController(StrategyAnalyticsService service){this.service=service;}
    @PostMapping("/scenario")
    public StrategyScenario scenario(@Valid @RequestBody StrategyScenarioRequest request){return service.scenario(request);}
}
