package com.marketintel.controller;
import com.marketintel.model.*;import com.marketintel.service.*;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;import java.time.Instant;import java.util.List;
@RestController @RequestMapping("/api/v3") @CrossOrigin(origins="http://localhost:4200") public class V3Controller {private final FeatureStoreService store;private final BacktestService backtest;private final PatternEngineService patterns;private final WalkForwardMlService ml;public V3Controller(FeatureStoreService s,BacktestService b,PatternEngineService p,WalkForwardMlService m){store=s;backtest=b;patterns=p;ml=m;}
 @PostMapping("/features/build") public List<FeaturePoint> build(@RequestParam String symbol,@RequestParam Instant from,@RequestParam Instant to){return store.buildAndStore(symbol,from,to);}
 @GetMapping("/features") public List<FeaturePoint> features(@RequestParam String symbol,@RequestParam Instant from,@RequestParam Instant to){return store.load(symbol,from,to);}
 @PostMapping("/backtest") public BacktestResult backtest(@Valid @RequestBody BacktestRequest request){return backtest.run(request);}
 @GetMapping("/patterns") public List<PatternSignal> patterns(@RequestParam String symbol,@RequestParam Instant from,@RequestParam Instant to){return this.patterns.detect(store.load(symbol,from,to));}
 @PostMapping("/ml/walk-forward") public WalkForwardResult ml(@RequestParam String symbol,@Valid @RequestBody WalkForwardRequest request){return ml.validate(symbol,request);}}
