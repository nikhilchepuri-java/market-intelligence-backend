package com.marketintel.controller;

import com.marketintel.model.AlertEvent;
import com.marketintel.model.AlertRule;
import com.marketintel.model.AlertRuleRequest;
import com.marketintel.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:4200")
public class AlertController {
    private final AlertService alerts;

    public AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertRule create(@Valid @RequestBody AlertRuleRequest request) {
        return alerts.create(request);
    }

    @GetMapping
    public List<AlertRule> list(@RequestParam(required = false) String symbol) {
        return alerts.list(symbol);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        alerts.delete(id);
    }

    @PostMapping("/evaluate/{symbol}")
    public List<AlertEvent> evaluate(@PathVariable String symbol) {
        return alerts.evaluate(symbol);
    }
}
