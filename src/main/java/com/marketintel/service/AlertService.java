package com.marketintel.service;

import com.marketintel.model.*;
import com.marketintel.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {
    private final AlertRuleRepository repository;
    private final TechnicalService technical;

    public AlertService(AlertRuleRepository repository, TechnicalService technical) {
        this.repository = repository;
        this.technical = technical;
    }

    @Transactional
    public AlertRule create(AlertRuleRequest request) {
        long cooldown = request.cooldownSeconds() == null ? 300 : request.cooldownSeconds();
        AlertRuleEntity entity = repository.save(new AlertRuleEntity(
                request.symbol().trim().toUpperCase(),
                request.type(),
                request.threshold(),
                request.enabled(),
                cooldown));
        return toModel(entity);
    }

    public List<AlertRule> list(String symbol) {
        return symbol == null || symbol.isBlank()
                ? repository.findAll().stream().map(this::toModel).toList()
                : repository.findBySymbolIgnoreCaseOrderByIdAsc(symbol.trim()).stream().map(this::toModel).toList();
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public List<AlertEvent> evaluate(String symbol) {
        TechnicalSnapshot snapshot = technical.snapshot(symbol);
        Instant now = Instant.now();

        return repository.findBySymbolIgnoreCaseOrderByIdAsc(symbol).stream()
                .filter(AlertRuleEntity::isEnabled)
                .filter(rule -> eligible(rule, now))
                .map(rule -> evaluateRule(rule, snapshot, now))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private AlertEvent evaluateRule(AlertRuleEntity rule, TechnicalSnapshot snapshot, Instant now) {
        double value = valueFor(rule, snapshot);
        boolean triggered = switch (rule.getType()) {
            case PRICE_ABOVE, RSI_ABOVE, RELATIVE_VOLUME_ABOVE -> value > rule.getThreshold();
            case PRICE_BELOW, RSI_BELOW -> value < rule.getThreshold();
            case BREAKS_RESISTANCE1 -> snapshot.price() > snapshot.resistance1();
            case BREAKS_SUPPORT1 -> snapshot.price() < snapshot.support1();
        };

        if (!triggered) {
            return null;
        }

        rule.setLastTriggeredAt(now);
        String message = switch (rule.getType()) {
            case PRICE_ABOVE -> String.format("%s price %.2f is above %.2f", snapshot.symbol(), value, rule.getThreshold());
            case PRICE_BELOW -> String.format("%s price %.2f is below %.2f", snapshot.symbol(), value, rule.getThreshold());
            case RSI_ABOVE -> String.format("%s RSI14 %.2f is above %.2f", snapshot.symbol(), value, rule.getThreshold());
            case RSI_BELOW -> String.format("%s RSI14 %.2f is below %.2f", snapshot.symbol(), value, rule.getThreshold());
            case RELATIVE_VOLUME_ABOVE -> String.format("%s relative volume %.2f is above %.2f", snapshot.symbol(), value, rule.getThreshold());
            case BREAKS_RESISTANCE1 -> String.format("%s price %.2f is above resistance1 %.2f", snapshot.symbol(), snapshot.price(), snapshot.resistance1());
            case BREAKS_SUPPORT1 -> String.format("%s price %.2f is below support1 %.2f", snapshot.symbol(), snapshot.price(), snapshot.support1());
        };
        return new AlertEvent(rule.getId(), snapshot.symbol(), rule.getType(), value, rule.getThreshold(), now, message);
    }

    private boolean eligible(AlertRuleEntity rule, Instant now) {
        return rule.getLastTriggeredAt() == null
                || !now.isBefore(rule.getLastTriggeredAt().plusSeconds(rule.getCooldownSeconds()));
    }

    private double valueFor(AlertRuleEntity rule, TechnicalSnapshot snapshot) {
        return switch (rule.getType()) {
            case PRICE_ABOVE, PRICE_BELOW, BREAKS_RESISTANCE1, BREAKS_SUPPORT1 -> snapshot.price();
            case RSI_ABOVE, RSI_BELOW -> snapshot.rsi14();
            case RELATIVE_VOLUME_ABOVE -> snapshot.relativeVolume20();
        };
    }

    private AlertRule toModel(AlertRuleEntity entity) {
        return new AlertRule(entity.getId(), entity.getSymbol(), entity.getType(),
                entity.getThreshold(), entity.isEnabled(), entity.getCooldownSeconds(), entity.getLastTriggeredAt());
    }
}
