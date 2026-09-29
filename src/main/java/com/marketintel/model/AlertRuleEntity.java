package com.marketintel.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "alert_rules")
public class AlertRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertRuleType type;

    @Column(nullable = false)
    private double threshold;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private long cooldownSeconds = 300;

    private Instant lastTriggeredAt;

    protected AlertRuleEntity() {
    }

    public AlertRuleEntity(String symbol, AlertRuleType type, double threshold, boolean enabled, long cooldownSeconds) {
        this.symbol = symbol;
        this.type = type;
        this.threshold = threshold;
        this.enabled = enabled;
        this.cooldownSeconds = cooldownSeconds;
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public AlertRuleType getType() { return type; }
    public double getThreshold() { return threshold; }
    public boolean isEnabled() { return enabled; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public Instant getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(Instant lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
}
