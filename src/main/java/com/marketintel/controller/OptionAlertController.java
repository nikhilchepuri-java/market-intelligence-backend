package com.marketintel.controller;

import com.marketintel.model.*;
import com.marketintel.service.OptionAlertService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/options/alerts")
@CrossOrigin(origins="http://localhost:4200")
public class OptionAlertController {
    private final OptionAlertService service;
    public OptionAlertController(OptionAlertService service){this.service=service;}
    @GetMapping
    public List<OptionAlertRule> list(@RequestParam(required=false) String underlying){return service.list(underlying);}
    @PostMapping
    public OptionAlertRule create(@RequestBody OptionAlertRule rule){return service.create(rule);}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){service.delete(id);}
    @PostMapping("/evaluate/{symbol}")
    public List<OptionAlertEvent> evaluate(@PathVariable String symbol,@RequestParam LocalDate expiration){return service.evaluate(symbol,expiration);}
}
