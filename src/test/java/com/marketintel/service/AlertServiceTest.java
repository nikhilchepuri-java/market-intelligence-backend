package com.marketintel.service;

import com.marketintel.model.*;
import com.marketintel.repository.AlertRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock
    AlertRuleRepository repository;

    @Mock
    TechnicalService technical;

    @InjectMocks
    AlertService service;

    @Test
    void evaluatesPriceAboveRule() {
        AlertRuleEntity rule = new AlertRuleEntity("NVDA", AlertRuleType.PRICE_ABOVE, 100.0, true, 300);
        when(repository.findBySymbolIgnoreCaseOrderByIdAsc("NVDA")).thenReturn(List.of(rule));
        when(technical.snapshot("NVDA")).thenReturn(snapshot("NVDA", 110.0));

        List<AlertEvent> events = service.evaluate("NVDA");

        assertEquals(1, events.size());
        assertEquals(AlertRuleType.PRICE_ABOVE, events.get(0).type());
        assertEquals(110.0, events.get(0).value());
        verify(repository, never()).save(any());
    }

    @Test
    void doesNotTriggerWhenPriceIsBelowThreshold() {
        AlertRuleEntity rule = new AlertRuleEntity("NVDA", AlertRuleType.PRICE_ABOVE, 100.0, true, 300);
        when(repository.findBySymbolIgnoreCaseOrderByIdAsc("NVDA")).thenReturn(List.of(rule));
        when(technical.snapshot("NVDA")).thenReturn(snapshot("NVDA", 90.0));

        assertTrue(service.evaluate("NVDA").isEmpty());
    }

    private TechnicalSnapshot snapshot(String symbol, double price) {
        return new TechnicalSnapshot(symbol, price, 95, 90, 80, 96, 94, 92,
                55, 3, 1000, 900, 1.11, 85, 80, 105, 110,
                "BULLISH", 5, 6);
    }
}
