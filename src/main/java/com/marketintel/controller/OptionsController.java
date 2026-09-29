package com.marketintel.controller;

import com.marketintel.model.*;
import com.marketintel.service.OptionAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/options")
@CrossOrigin(origins = "http://localhost:4200")
public class OptionsController {
    private final OptionAnalyticsService options;

    public OptionsController(OptionAnalyticsService options) {
        this.options = options;
    }

    @GetMapping("/{symbol}/expirations")
    public List<LocalDate> expirations(@PathVariable String symbol) {
        return options.expirations(symbol);
    }

    @GetMapping("/{symbol}/chain")
    public OptionChain chain(@PathVariable String symbol, @RequestParam LocalDate expiration) {
        return options.chain(symbol, expiration);
    }

    @GetMapping("/{symbol}/unusual")
    public List<UnusualOptionActivity> unusual(@PathVariable String symbol, @RequestParam LocalDate expiration) {
        return options.unusual(symbol, expiration);
    }

    @PostMapping("/scenario")
    public OptionScenario scenario(@Valid @RequestBody OptionScenarioRequest request) {
        return options.scenario(request);
    }
}
