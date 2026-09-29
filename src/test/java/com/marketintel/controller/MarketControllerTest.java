package com.marketintel.controller;

import com.marketintel.model.SupportResistance;
import com.marketintel.model.TechnicalSnapshot;
import com.marketintel.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketController.class)
class MarketControllerTest {
    @Autowired
    MockMvc mvc;

    @MockBean
    MarketDataProvider market;

    @MockBean
    TechnicalService technical;

    @MockBean
    SignalService signals;

    @MockBean
    NewsService news;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void levelsEndpointMapsTechnicalLevels() throws Exception {
        when(technical.snapshot("NVDA")).thenReturn(new TechnicalSnapshot(
                "NVDA", 140, 135, 130, 120, 138, 137, 133,
                60, 4, 1000, 900, 1.11, 125, 120, 145, 150,
                "BULLISH", 6, 6));

        mvc.perform(get("/api/levels/NVDA").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.support1").value(125.0))
                .andExpect(jsonPath("$.support2").value(120.0))
                .andExpect(jsonPath("$.resistance1").value(145.0))
                .andExpect(jsonPath("$.resistance2").value(150.0));
    }
}
